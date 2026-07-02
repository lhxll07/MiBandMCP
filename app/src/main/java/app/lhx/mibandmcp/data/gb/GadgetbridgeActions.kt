package app.lhx.mibandmcp.data.gb

object GadgetbridgeActions {
    val KnownPackages = listOf(
        "nodomain.freeyourgadget.gadgetbridge",
        "nodomain.freeyourgadget.gadgetbridge.nightly",
        "nodomain.freeyourgadget.gadgetbridge.nightly_nopebble",
        "com.espruino.gadgetbridge.banglejs",
        "com.espruino.gadgetbridge.banglejs.nightly",
    )

    const val CommandActivitySync =
        "nodomain.freeyourgadget.gadgetbridge.command.ACTIVITY_SYNC"
    const val CommandTriggerExport =
        "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_EXPORT"
    const val CommandTriggerDatabaseExport =
        "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_DATABASE_EXPORT"

    const val ActionSyncFinish =
        "nodomain.freeyourgadget.gadgetbridge.action.ACTIVITY_SYNC_FINISH"
    const val ActionExportSuccess =
        "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_SUCCESS"
    const val ActionExportFail =
        "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_FAIL"
}
