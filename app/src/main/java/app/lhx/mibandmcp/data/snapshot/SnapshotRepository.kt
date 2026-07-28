package app.lhx.mibandmcp.data.snapshot

import app.lhx.mibandmcp.model.AppSnapshot
import app.lhx.mibandmcp.model.EndpointInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SnapshotRepository {
    private val mutableSnapshot = MutableStateFlow(AppSnapshot())
    val snapshot: StateFlow<AppSnapshot> = mutableSnapshot.asStateFlow()

    fun setServiceRunning(isRunning: Boolean, endpoint: EndpointInfo?) {
        updateSnapshot { current ->
            current.copy(
                serviceStatus = current.serviceStatus.copy(
                    isRunning = isRunning,
                    endpoint = endpoint,
                    message = null,
                ),
            )
        }
    }

    fun setServiceError(message: String) {
        updateSnapshot { current ->
            current.copy(
                serviceStatus = current.serviceStatus.copy(
                    isRunning = false,
                    message = message,
                ),
            )
        }
    }

    fun updateIntegrationState(
        gadgetbridgeInstalled: Boolean,
        exportGranted: Boolean,
        detail: String,
    ) {
        updateSnapshot { current ->
            current.copy(
                bandStatus = current.bandStatus.copy(
                    gadgetbridgeInstalled = gadgetbridgeInstalled,
                    exportGranted = exportGranted,
                    detail = detail,
                ),
            )
        }
    }

    fun setRefreshing(message: String) {
        updateSnapshot { current ->
            current.copy(
                syncStatus = current.syncStatus.copy(
                    isRefreshing = true,
                    errorMessage = null,
                    statusMessage = message,
                ),
            )
        }
    }

    fun applyImportedSnapshot(imported: AppSnapshot) {
        updateSnapshot { current ->
            imported.copy(serviceStatus = current.serviceStatus)
        }
    }

    fun setRefreshError(
        gadgetbridgeInstalled: Boolean,
        exportGranted: Boolean,
        message: String,
    ) {
        updateSnapshot { current ->
            current.copy(
                bandStatus = current.bandStatus.copy(
                    gadgetbridgeInstalled = gadgetbridgeInstalled,
                    exportGranted = exportGranted,
                ),
                syncStatus = current.syncStatus.copy(
                    isRefreshing = false,
                    errorMessage = message,
                    statusMessage = null,
                ),
            )
        }
    }

    private inline fun updateSnapshot(transform: (AppSnapshot) -> AppSnapshot) {
        mutableSnapshot.update(transform)
    }
}
