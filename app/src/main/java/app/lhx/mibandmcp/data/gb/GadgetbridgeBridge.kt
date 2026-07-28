package app.lhx.mibandmcp.data.gb

import android.content.Intent
import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import app.lhx.mibandmcp.R
import app.lhx.mibandmcp.util.localizedString
import app.lhx.mibandmcp.data.prefs.SettingsStore
import app.lhx.mibandmcp.data.snapshot.SnapshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull

class GadgetbridgeBridge(
    context: Context,
    private val settingsStore: SettingsStore,
    private val snapshotRepository: SnapshotRepository,
    private val exportReader: GadgetbridgeExportReader,
) {
    private val logTag = "MiBandMCP/GBBridge"
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bridgeEvents = MutableSharedFlow<BridgeSignal>(extraBufferCapacity = 8)
    private val refreshMutex = Mutex()

    fun requestRefresh() {
        if (!refreshMutex.tryLock()) {
            Log.d(logTag, "requestRefresh() ignored: refresh already running")
            return
        }
        Log.d(logTag, "requestRefresh() queued")
        snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_queued))
        scope.launch {
            try {
                runCatching {
                    performRefresh()
                }.onFailure {
                    Log.e(logTag, "performRefresh() failed", it)
                    val preferences = settingsStore.preferences.first()
                    snapshotRepository.setRefreshError(
                        gadgetbridgeInstalled = findInstalledPackageName() != null,
                        exportGranted = !preferences.exportUri.isNullOrBlank(),
                        message = appContext.localizedString(
                            R.string.intent_request_failed,
                            it.message?.takeIf(String::isNotBlank) ?: it::class.java.simpleName,
                        ),
                    )
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    fun onBroadcastAction(action: String?) {
        Log.d(logTag, "onBroadcastAction action=$action")
        val event = when (action) {
            GadgetbridgeActions.ActionSyncFinish -> BridgeEvent.SyncFinished
            GadgetbridgeActions.ActionExportSuccess -> BridgeEvent.ExportSucceeded
            GadgetbridgeActions.ActionExportFail -> BridgeEvent.ExportFailed
            else -> null
        } ?: return
        bridgeEvents.tryEmit(BridgeSignal(type = event, receivedAtMillis = System.currentTimeMillis()))
    }

    fun isGadgetbridgeInstalled(): Boolean = findInstalledPackageName() != null

    private suspend fun performRefresh() {
        val packageName = findInstalledPackageName()
        val installed = packageName != null
        val preferences = settingsStore.preferences.first()
        val exportUri = preferences.exportUri
        var warningMessage: String? = null
        var completionMessage: String? = null
        var syncConfirmed = false
        var exportConfirmed = false
        var exportFileUpdated = false

        Log.d(
            logTag,
            "performRefresh() start installed=$installed packageName=$packageName exportUriPresent=${!exportUri.isNullOrBlank()}",
        )

        snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_preparing))
        snapshotRepository.updateIntegrationState(
            gadgetbridgeInstalled = installed,
            exportGranted = !exportUri.isNullOrBlank(),
            detail = snapshotRepository.snapshot.value.bandStatus.detail,
        )

        if (exportUri.isNullOrBlank()) {
            Log.w(logTag, "performRefresh() aborted: no export file selected")
            snapshotRepository.setRefreshError(
                gadgetbridgeInstalled = installed,
                exportGranted = false,
                message = appContext.localizedString(R.string.no_export_file_selected),
            )
            return
        }

        if (packageName != null) {
            val exportMetadataBefore = readExportMetadata(exportUri)
            snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_requesting_sync))
            val syncRequestStartedAt = System.currentTimeMillis()
            sendCommand(packageName, GadgetbridgeActions.CommandActivitySync)
            snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_waiting_sync))
            val syncResult = waitForEvent(
                expected = BridgeEvent.SyncFinished,
                notBeforeMillis = syncRequestStartedAt,
                timeoutMillis = SyncTimeoutMillis,
            )
            if (syncResult == null) {
                Log.w(logTag, "sync callback not confirmed within timeout")
                warningMessage = appContext.localizedString(R.string.sync_intent_not_confirmed)
            } else {
                Log.d(logTag, "sync callback confirmed")
                syncConfirmed = true
            }

            snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_requesting_export))
            val exportRequestStartedAt = System.currentTimeMillis()
            sendCommand(packageName, GadgetbridgeActions.CommandTriggerDatabaseExport)
            snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_waiting_export))
            when (waitForEvent(
                expected = BridgeEvent.ExportSucceeded,
                notBeforeMillis = exportRequestStartedAt,
                timeoutMillis = ExportTimeoutMillis,
            )) {
                BridgeEvent.ExportFailed -> {
                    Log.w(logTag, "export callback reported failure")
                    snapshotRepository.setRefreshError(
                        gadgetbridgeInstalled = true,
                        exportGranted = true,
                        message = appContext.localizedString(R.string.export_failed),
                    )
                    return
                }

                BridgeEvent.ExportSucceeded -> {
                    Log.d(logTag, "export callback confirmed")
                    exportConfirmed = true
                }
                null -> {
                    Log.w(logTag, "export callback not confirmed within timeout")
                    if (warningMessage == null) {
                        warningMessage = appContext.localizedString(R.string.export_intent_not_confirmed)
                    }
                }
                else -> Unit
            }

            exportFileUpdated = hasExportFileChanged(
                uriString = exportUri,
                previous = exportMetadataBefore,
            )
            Log.d(logTag, "export file changed=$exportFileUpdated")

            if (exportFileUpdated) {
                warningMessage = null
            }
        }

        snapshotRepository.setRefreshing(appContext.localizedString(R.string.refresh_stage_reading_export))
        runCatching {
            exportReader.readFromUri(exportUri)
        }.onSuccess { imported ->
            completionMessage = when {
                !installed -> appContext.localizedString(R.string.refresh_result_export_only)
                syncConfirmed && exportConfirmed -> appContext.localizedString(R.string.refresh_result_intent_ok)
                exportFileUpdated && !syncConfirmed && !exportConfirmed ->
                    appContext.localizedString(R.string.refresh_result_file_updated_without_callbacks)
                exportFileUpdated && syncConfirmed && !exportConfirmed ->
                    appContext.localizedString(R.string.refresh_result_file_updated_without_export_callback)
                exportFileUpdated && !syncConfirmed && exportConfirmed ->
                    appContext.localizedString(R.string.refresh_result_file_updated_without_sync_callback)
                !syncConfirmed && exportConfirmed -> appContext.localizedString(R.string.refresh_result_export_callback_only)
                syncConfirmed && !exportConfirmed -> appContext.localizedString(R.string.refresh_result_sync_callback_only)
                else -> appContext.localizedString(R.string.refresh_result_fallback_export)
            }
            snapshotRepository.applyImportedSnapshot(
                imported.copy(
                    bandStatus = imported.bandStatus.copy(
                        gadgetbridgeInstalled = installed,
                        exportGranted = true,
                    ),
                    syncStatus = imported.syncStatus.copy(
                        errorMessage = warningMessage,
                        statusMessage = completionMessage,
                    ),
                ),
            )
            Log.d(
                logTag,
                "read export success syncConfirmed=$syncConfirmed exportConfirmed=$exportConfirmed exportFileUpdated=$exportFileUpdated warning=$warningMessage completion=$completionMessage",
            )
        }.onFailure {
            Log.e(logTag, "read export failed", it)
            snapshotRepository.setRefreshError(
                gadgetbridgeInstalled = installed,
                exportGranted = true,
                message = appContext.localizedString(R.string.failed_to_read_export),
            )
        }
    }

    private fun sendCommand(packageName: String, action: String) {
        val intent = Intent(action)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        Log.d(logTag, "sendBroadcast package=$packageName action=$action")
        appContext.sendBroadcast(intent)
    }

    private suspend fun waitForEvent(
        expected: BridgeEvent,
        notBeforeMillis: Long,
        timeoutMillis: Long,
    ): BridgeEvent? {
        return withTimeoutOrNull(timeoutMillis) {
            bridgeEvents
                .filter { signal ->
                    signal.receivedAtMillis >= notBeforeMillis &&
                        (signal.type == expected || signal.type == BridgeEvent.ExportFailed)
                }
                .firstOrNull()
                ?.type
        }
    }

    private fun findInstalledPackageName(): String? {
        return GadgetbridgeActions.KnownPackages.firstOrNull { packageName ->
            runCatching {
                appContext.packageManager.getPackageInfo(packageName, 0)
            }.isSuccess
        }
    }

    private fun hasExportFileChanged(
        uriString: String,
        previous: ExportMetadata?,
    ): Boolean {
        val current = readExportMetadata(uriString) ?: return false
        if (previous == null) return current.lastModifiedMillis != null || current.sizeBytes != null
        return current.lastModifiedMillis != previous.lastModifiedMillis ||
            current.sizeBytes != previous.sizeBytes
    }

    private fun readExportMetadata(uriString: String): ExportMetadata? {
        val uri = uriString.toUri()
        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val lastModifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                ExportMetadata(
                    lastModifiedMillis = if (lastModifiedIndex >= 0 && !cursor.isNull(lastModifiedIndex)) {
                        cursor.getLong(lastModifiedIndex)
                    } else {
                        null
                    },
                    sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        null
                    },
                )
            }
        }.onFailure {
            Log.w(logTag, "failed to read export metadata", it)
        }.getOrNull()
    }

    private enum class BridgeEvent {
        SyncFinished,
        ExportSucceeded,
        ExportFailed,
    }

    private data class BridgeSignal(
        val type: BridgeEvent,
        val receivedAtMillis: Long,
    )

    private data class ExportMetadata(
        val lastModifiedMillis: Long?,
        val sizeBytes: Long?,
    )

    companion object {
        private const val SyncTimeoutMillis = 6_000L
        private const val ExportTimeoutMillis = 4_000L
    }
}
