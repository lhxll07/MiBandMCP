package app.lhx.mibandmcp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lhx.mibandmcp.R
import app.lhx.mibandmcp.ui.HomeUiState
import app.lhx.mibandmcp.util.TimeFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRefresh: () -> Unit,
    onSelectExportFile: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.app_name))
                        Text(
                            text = if (uiState.serviceRunning) {
                                stringResource(R.string.service_running)
                            } else {
                                stringResource(R.string.service_stopped)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.open_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ServiceCard(
                isRunning = uiState.serviceRunning,
                endpoint = uiState.endpointUrl,
                message = uiState.serviceMessage,
            )
            DataSourceCard(
                installed = uiState.gadgetbridgeInstalled,
                exportGranted = uiState.exportGranted,
                dataReady = uiState.dataReady,
                deviceLabel = uiState.deviceLabel,
                lastSync = TimeFormatters.relativeTime(context, uiState.lastSyncEpochMillis),
                isRefreshing = uiState.isRefreshing,
                errorMessage = uiState.refreshErrorMessage,
                statusMessage = uiState.refreshStatusMessage,
            )
            HealthSummaryCard(
                steps = uiState.stepsToday,
                bpm = uiState.latestHeartRate,
                heartRateTime = TimeFormatters.relativeTime(context, uiState.latestHeartRateEpochMillis),
                sleepHours = TimeFormatters.sleepDuration(uiState.sleepTotalMinutes),
                sleepWindow = "${uiState.fellAsleepLabel} - ${uiState.wokeUpLabel}",
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.serviceRunning) {
                    FilledTonalButton(
                        onClick = onStopService,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Text(
                            text = stringResource(R.string.stop_service),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    Button(
                        onClick = onStartService,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(
                            text = stringResource(R.string.start_service),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (uiState.exportGranted) {
                    FilledTonalButton(
                        onClick = onRefresh,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text(
                            text = if (uiState.isRefreshing) {
                                stringResource(R.string.refreshing)
                            } else {
                                stringResource(R.string.refresh_now)
                            },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onSelectExportFile,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                        Text(
                            text = stringResource(R.string.select_export_file),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    isRunning: Boolean,
    endpoint: String?,
    message: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.service_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        if (isRunning) {
                            stringResource(R.string.service_running)
                        } else {
                            stringResource(R.string.service_stopped)
                        },
                    )
                },
                colors = AssistChipDefaults.assistChipColors(),
            )
            Text(
                text = endpoint ?: stringResource(R.string.endpoint_unavailable),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DataSourceCard(
    installed: Boolean,
    exportGranted: Boolean,
    dataReady: Boolean,
    deviceLabel: String,
    lastSync: String,
    isRefreshing: Boolean,
    errorMessage: String?,
    statusMessage: String?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.data_source_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        if (installed) {
                            stringResource(R.string.gadgetbridge_ready)
                        } else {
                            stringResource(R.string.gadgetbridge_missing)
                        },
                    )
                },
            )
            Text(
                text = when {
                    !installed -> stringResource(R.string.gadgetbridge_missing_detail)
                    !exportGranted -> stringResource(R.string.export_access_missing_detail)
                    !errorMessage.isNullOrBlank() -> errorMessage
                    deviceLabel.isNotBlank() -> deviceLabel
                    dataReady -> stringResource(R.string.data_source_ready_detail)
                    else -> stringResource(R.string.data_source_demo_detail)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (isRefreshing) {
                    statusMessage ?: stringResource(R.string.refreshing)
                } else {
                    stringResource(R.string.last_sync_format, lastSync)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isRefreshing && !statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HealthSummaryCard(
    steps: Int,
    bpm: Int,
    heartRateTime: String,
    sleepHours: String,
    sleepWindow: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.health_summary_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            MetricBlock(
                label = stringResource(R.string.steps_today),
                value = steps.toString(),
                supporting = stringResource(R.string.steps_unit),
            )
            MetricBlock(
                label = stringResource(R.string.latest_heart_rate),
                value = bpm.toString(),
                supporting = stringResource(R.string.heart_rate_supporting, heartRateTime),
            )
            MetricBlock(
                label = stringResource(R.string.last_night_sleep),
                value = sleepHours,
                supporting = sleepWindow,
            )
        }
    }
}

@Composable
private fun MetricBlock(
    label: String,
    value: String,
    supporting: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
