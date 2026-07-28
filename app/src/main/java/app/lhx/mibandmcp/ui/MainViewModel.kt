package app.lhx.mibandmcp.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lhx.mibandmcp.appContainer
import app.lhx.mibandmcp.data.prefs.AppPreferences
import app.lhx.mibandmcp.service.McpForegroundService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.Home,
    val home: HomeUiState = HomeUiState(),
    val settings: SettingsUiState = SettingsUiState(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val logTag = "MiBandMCP/MainVM"
    private val container = application.appContainer
    private val app = application
    private val screenFlow = MutableStateFlow(AppScreen.Home)

    val uiState: StateFlow<MainUiState> = combine(
        screenFlow,
        container.settingsStore.preferences,
        container.snapshotRepository.snapshot,
    ) { screen, preferences, snapshot ->
        MainUiState(
            currentScreen = screen,
            home = HomeUiState(
                serviceRunning = snapshot.serviceStatus.isRunning,
                endpointUrl = snapshot.serviceStatus.endpoint?.url,
                serviceMessage = snapshot.serviceStatus.message,
                gadgetbridgeInstalled = snapshot.bandStatus.gadgetbridgeInstalled,
                exportGranted = snapshot.bandStatus.exportGranted,
                dataReady = snapshot.bandStatus.dataReady,
                deviceLabel = snapshot.bandStatus.detail,
                isRefreshing = snapshot.syncStatus.isRefreshing,
                refreshErrorMessage = snapshot.syncStatus.errorMessage,
                refreshStatusMessage = snapshot.syncStatus.statusMessage,
                lastSyncEpochMillis = snapshot.syncStatus.lastSyncEpochMillis,
                stepsToday = snapshot.activitySummary.stepsToday,
                latestHeartRate = snapshot.heartRateSample.bpm,
                latestHeartRateEpochMillis = snapshot.heartRateSample.measuredAtEpochMillis,
                sleepTotalMinutes = snapshot.sleepSummary.totalMinutes,
                fellAsleepLabel = snapshot.sleepSummary.fellAsleepLabel,
                wokeUpLabel = snapshot.sleepSummary.wokeUpLabel,
            ),
            settings = SettingsUiState(
                port = preferences.port,
                exportUri = preferences.exportUri,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            val preferences = container.settingsStore.preferences.first()
            container.snapshotRepository.updateIntegrationState(
                gadgetbridgeInstalled = container.gadgetbridgeBridge.isGadgetbridgeInstalled(),
                exportGranted = !preferences.exportUri.isNullOrBlank(),
                detail = container.snapshotRepository.snapshot.value.bandStatus.detail,
            )
        }
    }

    fun openSettings() {
        screenFlow.value = AppScreen.Settings
    }

    fun openHome() {
        screenFlow.value = AppScreen.Home
    }

    fun startService() {
        McpForegroundService.start(app)
    }

    fun stopService() {
        McpForegroundService.stop(app)
    }

    fun refreshNow() {
        Log.d(logTag, "refreshNow() called")
        container.gadgetbridgeBridge.requestRefresh()
    }

    fun onExportFileSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            container.settingsStore.setExportUri(uri.toString())
            refreshNow()
        }
    }

    fun setPort(rawPort: String) {
        val port = rawPort.toIntOrNull() ?: return
        if (port !in 1024..65535) return
        viewModelScope.launch {
            container.settingsStore.setPort(port)
            val snapshot = container.snapshotRepository.snapshot.value
            if (snapshot.serviceStatus.isRunning) {
                McpForegroundService.stop(app)
                McpForegroundService.start(app)
            }
        }
    }

}

data class HomeUiState(
    val serviceRunning: Boolean = false,
    val endpointUrl: String? = null,
    val serviceMessage: String? = null,
    val gadgetbridgeInstalled: Boolean = false,
    val exportGranted: Boolean = false,
    val dataReady: Boolean = false,
    val deviceLabel: String = "",
    val isRefreshing: Boolean = false,
    val refreshErrorMessage: String? = null,
    val refreshStatusMessage: String? = null,
    val lastSyncEpochMillis: Long? = null,
    val stepsToday: Int = 0,
    val latestHeartRate: Int = 0,
    val latestHeartRateEpochMillis: Long? = null,
    val sleepTotalMinutes: Int = 0,
    val fellAsleepLabel: String = "--:--",
    val wokeUpLabel: String = "--:--",
)

data class SettingsUiState(
    val port: Int = AppPreferences().port,
    val exportUri: String? = null,
)
