package app.lhx.mibandmcp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lhx.mibandmcp.R
import app.lhx.mibandmcp.ui.home.HomeScreen
import app.lhx.mibandmcp.ui.settings.AppLanguage
import app.lhx.mibandmcp.ui.settings.SettingsScreen

@Composable
fun MiBandMcpApp(
    viewModel: MainViewModel,
    onSelectExportFile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.Home,
                    onClick = viewModel::openHome,
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) },
                    colors = navigationColors(),
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.Settings,
                    onClick = viewModel::openSettings,
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                    colors = navigationColors(),
                )
            }
        },
    ) { contentPadding ->
        when (uiState.currentScreen) {
            AppScreen.Home -> HomeScreen(
                uiState = uiState.home,
                onStartService = viewModel::startService,
                onStopService = viewModel::stopService,
                onRefresh = viewModel::refreshNow,
                onSelectExportFile = onSelectExportFile,
                modifier = Modifier.padding(contentPadding),
            )

            AppScreen.Settings -> SettingsScreen(
                uiState = uiState.settings,
                selectedLanguage = AppLanguage.current(),
                onPortChange = viewModel::setPort,
                onSelectExportFile = onSelectExportFile,
                onLanguageChange = AppLanguage::apply,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun navigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
