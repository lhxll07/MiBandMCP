package app.lhx.mibandmcp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "miband_mcp_settings")

data class AppPreferences(
    val port: Int = 8787,
    val exportUri: String? = null,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val Port = intPreferencesKey("server_port")
        val ExportUri = stringPreferencesKey("export_uri")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs: Preferences ->
        AppPreferences(
            port = prefs[Keys.Port] ?: 8787,
            exportUri = prefs[Keys.ExportUri],
        )
    }

    suspend fun setPort(port: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.Port] = port
        }
    }

    suspend fun setExportUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(Keys.ExportUri)
            } else {
                prefs[Keys.ExportUri] = uri
            }
        }
    }
}
