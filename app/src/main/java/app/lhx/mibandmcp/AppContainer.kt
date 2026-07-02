package app.lhx.mibandmcp

import android.app.Application
import android.content.Context
import app.lhx.mibandmcp.data.gb.GadgetbridgeBridge
import app.lhx.mibandmcp.data.gb.GadgetbridgeExportReader
import app.lhx.mibandmcp.data.prefs.SettingsStore
import app.lhx.mibandmcp.data.snapshot.SnapshotRepository
import app.lhx.mibandmcp.mcp.McpServerManager

class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context)
    val snapshotRepository = SnapshotRepository()
    val exportReader = GadgetbridgeExportReader(context)
    val gadgetbridgeBridge = GadgetbridgeBridge(context, settingsStore, snapshotRepository, exportReader)
    val mcpServerManager = McpServerManager(snapshotRepository) {
        gadgetbridgeBridge.requestRefresh()
    }
}

class MiBandMcpApplication : Application() {
    val appContainer by lazy { AppContainer(this) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MiBandMcpApplication).appContainer
