package app.lhx.mibandmcp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lhx.mibandmcp.BuildConfig
import app.lhx.mibandmcp.R
import app.lhx.mibandmcp.ui.SettingsUiState
import app.lhx.mibandmcp.ui.components.GroupDivider
import app.lhx.mibandmcp.ui.components.PageHeader
import app.lhx.mibandmcp.ui.components.SectionLabel
import app.lhx.mibandmcp.ui.components.SettingRow
import app.lhx.mibandmcp.ui.components.SurfaceGroup

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    selectedLanguage: AppLanguage,
    onPortChange: (String) -> Unit,
    onSelectExportFile: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var portText by remember(uiState.port) { mutableStateOf(uiState.port.toString()) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            PageHeader(
                title = stringResource(R.string.settings),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }
        item { SectionLabel(stringResource(R.string.general_section_title)) }
        item {
            SurfaceGroup {
                SettingRow(
                    title = stringResource(R.string.language_title),
                    supporting = stringResource(R.string.language_description),
                    leading = { Icon(Icons.Rounded.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                ) {
                    Box {
                        FilledTonalButton(onClick = { languageMenuExpanded = true }) {
                            Text(
                                text = languageLabel(selectedLanguage),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false },
                        ) {
                            AppLanguage.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(languageLabel(language)) },
                                    trailingIcon = {
                                        if (language == selectedLanguage) {
                                            Icon(Icons.Rounded.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        languageMenuExpanded = false
                                        onLanguageChange(language)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.service_section_title)) }
        item {
            SurfaceGroup {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = {
                            portText = it.filter(Char::isDigit).take(5)
                            onPortChange(portText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.server_port)) },
                        supportingText = { Text(stringResource(R.string.port_supporting_text)) },
                        leadingIcon = { Icon(Icons.Rounded.Router, contentDescription = null) },
                        singleLine = true,
                    )
                }
            }
        }
        item { SectionLabel(stringResource(R.string.data_source_section_title)) }
        item {
            SurfaceGroup {
                SettingRow(
                    title = stringResource(R.string.export_file_title),
                    supporting = uiState.exportUri ?: stringResource(R.string.no_export_file_selected),
                    leading = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                ) {
                    FilledTonalButton(onClick = onSelectExportFile) {
                        Text(stringResource(R.string.choose))
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.integration_section_title)) }
        item {
            SurfaceGroup {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        text = stringResource(R.string.intent_api_checklist_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.intent_api_checklist_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GroupDivider()
                SettingRow(
                    title = stringResource(R.string.app_version),
                    supporting = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                ) {
                    Text(
                        text = "MCP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.System -> R.string.language_system
        AppLanguage.English -> R.string.language_english
        AppLanguage.SimplifiedChinese -> R.string.language_simplified_chinese
    },
)
