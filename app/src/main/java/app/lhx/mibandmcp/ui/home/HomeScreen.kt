package app.lhx.mibandmcp.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lhx.mibandmcp.R
import app.lhx.mibandmcp.ui.HomeUiState
import app.lhx.mibandmcp.ui.components.PageHeader
import app.lhx.mibandmcp.ui.components.SectionLabel
import app.lhx.mibandmcp.ui.components.StatusPill
import app.lhx.mibandmcp.ui.components.StatusTone
import app.lhx.mibandmcp.util.TimeFormatters

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRefresh: () -> Unit,
    onSelectExportFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lastSync = TimeFormatters.relativeTime(context, uiState.lastSyncEpochMillis)
    val endpointClipLabel = stringResource(R.string.mcp_endpoint_clip_label)
    val endpointCopiedMessage = stringResource(R.string.endpoint_copied)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.home_subtitle),
                trailing = {
                    StatusPill(
                        text = stringResource(
                            if (uiState.serviceRunning) R.string.service_running else R.string.service_stopped,
                        ),
                        tone = if (uiState.serviceRunning) StatusTone.Positive else StatusTone.Neutral,
                    )
                },
            )
        }
        item {
            ServicePanel(
                isRunning = uiState.serviceRunning,
                endpoint = uiState.endpointUrl,
                message = uiState.serviceMessage,
                onStart = onStartService,
                onStop = onStopService,
                onCopyEndpoint = { endpoint ->
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(endpointClipLabel, endpoint),
                    )
                    Toast.makeText(context, endpointCopiedMessage, Toast.LENGTH_SHORT).show()
                },
            )
        }
        item {
            SectionLabel(
                title = stringResource(R.string.health_summary_card_title),
                supporting = stringResource(R.string.health_summary_supporting),
            )
        }
        item {
            MetricGrid(
                steps = uiState.stepsToday,
                bpm = uiState.latestHeartRate,
                heartRateTime = TimeFormatters.relativeTime(context, uiState.latestHeartRateEpochMillis),
                sleepHours = TimeFormatters.sleepDuration(context, uiState.sleepTotalMinutes),
                sleepWindow = "${uiState.fellAsleepLabel} - ${uiState.wokeUpLabel}",
            )
        }
        item {
            SectionLabel(title = stringResource(R.string.data_source_card_title))
        }
        item {
            DataSourcePanel(
                installed = uiState.gadgetbridgeInstalled,
                exportGranted = uiState.exportGranted,
                dataReady = uiState.dataReady,
                deviceLabel = uiState.deviceLabel,
                lastSync = lastSync,
                isRefreshing = uiState.isRefreshing,
                errorMessage = uiState.refreshErrorMessage,
                statusMessage = uiState.refreshStatusMessage,
                onRefresh = onRefresh,
                onSelectExportFile = onSelectExportFile,
            )
        }
    }
}

@Composable
private fun ServicePanel(
    isRunning: Boolean,
    endpoint: String?,
    message: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCopyEndpoint: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lan,
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp).size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.service_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = endpoint ?: stringResource(R.string.endpoint_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Text(stringResource(R.string.stop_service), modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedIconButton(
                        onClick = { endpoint?.let(onCopyEndpoint) },
                        enabled = endpoint != null,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.copy_endpoint),
                        )
                    }
                }
            } else {
                Button(onClick = onStart) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text(stringResource(R.string.start_service), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(
    steps: Int,
    bpm: Int,
    heartRateTime: String,
    sleepHours: String,
    sleepWindow: String,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 480.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(Icons.AutoMirrored.Rounded.DirectionsWalk, stringResource(R.string.steps_today), steps.toString(), stringResource(R.string.steps_unit))
                MetricTile(Icons.Rounded.FavoriteBorder, stringResource(R.string.latest_heart_rate), bpm.toString(), stringResource(R.string.heart_rate_supporting, heartRateTime))
                MetricTile(Icons.Rounded.Bedtime, stringResource(R.string.last_night_sleep), sleepHours, sleepWindow)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(Icons.AutoMirrored.Rounded.DirectionsWalk, stringResource(R.string.steps_today), steps.toString(), stringResource(R.string.steps_unit), Modifier.weight(1f))
                MetricTile(Icons.Rounded.FavoriteBorder, stringResource(R.string.latest_heart_rate), bpm.toString(), stringResource(R.string.heart_rate_supporting, heartRateTime), Modifier.weight(1f))
                MetricTile(Icons.Rounded.Bedtime, stringResource(R.string.last_night_sleep), sleepHours, sleepWindow, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DataSourcePanel(
    installed: Boolean,
    exportGranted: Boolean,
    dataReady: Boolean,
    deviceLabel: String,
    lastSync: String,
    isRefreshing: Boolean,
    errorMessage: String?,
    statusMessage: String?,
    onRefresh: () -> Unit,
    onSelectExportFile: () -> Unit,
) {
    val tone = when {
        !errorMessage.isNullOrBlank() -> StatusTone.Error
        installed && exportGranted && dataReady -> StatusTone.Positive
        !installed || !exportGranted -> StatusTone.Warning
        else -> StatusTone.Neutral
    }
    val statusText = when {
        !installed -> stringResource(R.string.gadgetbridge_missing)
        !exportGranted -> stringResource(R.string.export_access_required)
        dataReady -> stringResource(R.string.data_ready)
        else -> stringResource(R.string.data_waiting)
    }
    val detail = when {
        !installed -> stringResource(R.string.gadgetbridge_missing_detail)
        !exportGranted -> stringResource(R.string.export_access_missing_detail)
        !errorMessage.isNullOrBlank() -> errorMessage.orEmpty()
        deviceLabel.isNotBlank() -> deviceLabel
        dataReady -> stringResource(R.string.data_source_ready_detail)
        else -> stringResource(R.string.data_source_demo_detail)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = if (deviceLabel.isNotBlank()) deviceLabel else stringResource(R.string.gadgetbridge_label),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(statusText, tone)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (isRefreshing) statusMessage ?: stringResource(R.string.refreshing) else stringResource(R.string.last_sync_format, lastSync),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (exportGranted) {
                FilledTonalButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text(stringResource(R.string.refresh_now), modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                FilledTonalButton(onClick = onSelectExportFile) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Text(stringResource(R.string.select_export_file), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
