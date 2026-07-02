package app.lhx.mibandmcp.data.gb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.lhx.mibandmcp.appContainer

class GadgetbridgeBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MiBandMCP/GBReceiver", "onReceive action=${intent.action}")
        val container = context.appContainer
        when (intent.action) {
            GadgetbridgeActions.ActionSyncFinish,
            GadgetbridgeActions.ActionExportSuccess,
            GadgetbridgeActions.ActionExportFail,
            -> container.gadgetbridgeBridge.onBroadcastAction(intent.action)
        }
    }
}
