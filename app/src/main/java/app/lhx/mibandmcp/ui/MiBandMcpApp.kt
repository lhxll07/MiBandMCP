package app.lhx.mibandmcp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lhx.mibandmcp.ui.home.HomeScreen
import app.lhx.mibandmcp.ui.settings.SettingsScreen

@Composable
fun MiBandMcpApp(
    viewModel: MainViewModel,
    onSelectExportFile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState.currentScreen) {
        AppScreen.Home -> HomeScreen(
            uiState = uiState.home,
            onOpenSettings = viewModel::openSettings,
            onStartService = viewModel::startService,
            onStopService = viewModel::stopService,
            onRefresh = viewModel::refreshNow,
            onSelectExportFile = onSelectExportFile,
        )

        AppScreen.Settings -> SettingsScreen(
            uiState = uiState.settings,
            onBack = viewModel::openHome,
            onPortChange = viewModel::setPort,
            onAutoRefreshChange = viewModel::setAutoRefresh,
            onSelectExportFile = onSelectExportFile,
        )
    }
}
