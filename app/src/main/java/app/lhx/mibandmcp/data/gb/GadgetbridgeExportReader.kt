package app.lhx.mibandmcp.data.gb

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import app.lhx.mibandmcp.model.ActivitySummary
import app.lhx.mibandmcp.model.BatteryStatus
import app.lhx.mibandmcp.model.BandStatus
import app.lhx.mibandmcp.model.DailyMetrics
import app.lhx.mibandmcp.model.DeviceProfile
import app.lhx.mibandmcp.model.HeartRateSample
import app.lhx.mibandmcp.model.SleepSummary
import app.lhx.mibandmcp.model.StressSample
import app.lhx.mibandmcp.model.SyncStatus
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class ImportedBandSnapshot(
    val bandStatus: BandStatus,
    val deviceProfile: DeviceProfile,
    val activitySummary: ActivitySummary,
    val dailyMetrics: DailyMetrics,
    val heartRateSample: HeartRateSample,
    val batteryStatus: BatteryStatus,
    val stressSample: StressSample,
    val sleepSummary: SleepSummary,
    val syncStatus: SyncStatus,
)

class GadgetbridgeExportReader(context: Context) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.cacheDir, "gadgetbridge-import.sqlite3")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun readFromUri(uriString: String): ImportedBandSnapshot {
        val uri = Uri.parse(uriString)
        copyToCache(uri)
        val db = SQLiteDatabase.openDatabase(cacheFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use { database ->
            val device = readDevice(database)
            val summary = readDailySummary(database, device.id)
            val heartRate = readLatestHeartRate(database, device.id)
            val battery = readLatestBatteryStatus(database, device.id)
            val stress = readLatestStress(database, device.id)
            val sleep = readLatestSleep(database, device.id)
            val firmwareVersion = readFirmwareVersion(database, device.id)
            val lastSync = listOf(
                summary.timestampMillis,
                heartRate.measuredAtEpochMillis,
                battery.measuredAtEpochMillis,
                stress.measuredAtEpochMillis,
                sleep.updatedAtEpochMillis,
            ).filterNotNull().maxOrNull()

            return ImportedBandSnapshot(
                bandStatus = BandStatus(
                    gadgetbridgeInstalled = true,
                    exportGranted = true,
                    dataReady = true,
                    detail = device.label,
                ),
                deviceProfile = DeviceProfile(
                    name = device.name,
                    manufacturer = device.manufacturer,
                    model = device.model,
                    alias = device.alias,
                    typeName = device.typeName,
                    firmwareVersion = firmwareVersion,
                ),
                activitySummary = ActivitySummary(
                    stepsToday = summary.steps,
                ),
                dailyMetrics = DailyMetrics(
                    summaryEpochMillis = summary.timestampMillis,
                    caloriesToday = summary.calories,
                    restingHeartRate = summary.restingHeartRate,
                    averageHeartRate = summary.averageHeartRate,
                    maxHeartRate = summary.maxHeartRate,
                    maxHeartRateAtEpochMillis = summary.maxHeartRateAtEpochMillis,
                    minHeartRate = summary.minHeartRate,
                    minHeartRateAtEpochMillis = summary.minHeartRateAtEpochMillis,
                    averageStress = summary.averageStress,
                    maxStress = summary.maxStress,
                    minStress = summary.minStress,
                    averageSpo2 = summary.averageSpo2,
                    maxSpo2 = summary.maxSpo2,
                    maxSpo2AtEpochMillis = summary.maxSpo2AtEpochMillis,
                    minSpo2 = summary.minSpo2,
                    minSpo2AtEpochMillis = summary.minSpo2AtEpochMillis,
                    vitalityCurrent = summary.vitalityCurrent,
                ),
                heartRateSample = heartRate,
                batteryStatus = battery,
                stressSample = stress,
                sleepSummary = sleep,
                syncStatus = SyncStatus(
                    isRefreshing = false,
                    lastSyncEpochMillis = lastSync,
                    statusMessage = null,
                ),
            )
        }
    }

    private fun copyToCache(uri: Uri) {
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Unable to open selected export file")
    }

    private fun readDevice(db: SQLiteDatabase): DeviceInfo {
        db.rawQuery(
            """
            SELECT _id, NAME, MODEL
            , MANUFACTURER, ALIAS, TYPE_NAME
            FROM DEVICE
            ORDER BY _id ASC
            LIMIT 1
            """.trimIndent(),
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) error("No device found in export")
            val id = cursor.getLong(0)
            val name = cursor.getString(1).orEmpty()
            val model = cursor.getString(2).orEmpty()
            val manufacturer = cursor.getString(3).orEmpty()
            val alias = cursor.getString(4)
            val typeName = cursor.getString(5).orEmpty()
            val label = listOf(name, model).filter { it.isNotBlank() }.distinct().joinToString(" • ")
            return DeviceInfo(
                id = id,
                label = label.ifBlank { "Xiaomi device" },
                name = name,
                manufacturer = manufacturer,
                model = model,
                alias = alias,
                typeName = typeName,
            )
        }
    }

    private fun readFirmwareVersion(db: SQLiteDatabase, deviceId: Long): String? {
        db.rawQuery(
            """
            SELECT FIRMWARE_VERSION1
            FROM DEVICE_ATTRIBUTES
            WHERE DEVICE_ID = ?
            ORDER BY COALESCE(VALID_TO_UTC, 9223372036854775807) DESC, VALID_FROM_UTC DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(deviceId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(0)?.takeIf { it.isNotBlank() && it != "N/A" }
        }
    }

    private fun readDailySummary(db: SQLiteDatabase, deviceId: Long): DailySummary {
        db.rawQuery(
            """
            SELECT
                TIMESTAMP,
                COALESCE(STEPS, 0),
                COALESCE(CALORIES, 0),
                COALESCE(HR_RESTING, 0),
                COALESCE(HR_AVG, 0),
                COALESCE(HR_MAX, 0),
                HR_MAX_TS,
                COALESCE(HR_MIN, 0),
                HR_MIN_TS,
                COALESCE(STRESS_AVG, 0),
                COALESCE(STRESS_MAX, 0),
                COALESCE(STRESS_MIN, 0),
                COALESCE(SPO2_AVG, 0),
                COALESCE(SPO2_MAX, 0),
                SPO2_MAX_TS,
                COALESCE(SPO2_MIN, 0),
                SPO2_MIN_TS,
                COALESCE(VITALITY_CURRENT, 0)
            FROM XIAOMI_DAILY_SUMMARY_SAMPLE
            WHERE DEVICE_ID = ?
            ORDER BY TIMESTAMP DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(deviceId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return DailySummary()
            }
            return DailySummary(
                timestampMillis = normalizeTimestamp(cursor.getLong(0)),
                steps = cursor.getInt(1),
                calories = cursor.getInt(2),
                restingHeartRate = cursor.getInt(3),
                averageHeartRate = cursor.getInt(4),
                maxHeartRate = cursor.getInt(5),
                maxHeartRateAtEpochMillis = cursor.longOrNull(6)?.let(::normalizeTimestamp),
                minHeartRate = cursor.getInt(7),
                minHeartRateAtEpochMillis = cursor.longOrNull(8)?.let(::normalizeTimestamp),
                averageStress = cursor.getInt(9),
                maxStress = cursor.getInt(10),
                minStress = cursor.getInt(11),
                averageSpo2 = cursor.getInt(12),
                maxSpo2 = cursor.getInt(13),
                maxSpo2AtEpochMillis = cursor.longOrNull(14)?.let(::normalizeTimestamp),
                minSpo2 = cursor.getInt(15),
                minSpo2AtEpochMillis = cursor.longOrNull(16)?.let(::normalizeTimestamp),
                vitalityCurrent = cursor.getInt(17),
            )
        }
    }

    private fun readLatestHeartRate(db: SQLiteDatabase, deviceId: Long): HeartRateSample {
        db.rawQuery(
            """
            SELECT TIMESTAMP, HEART_RATE
            FROM XIAOMI_ACTIVITY_SAMPLE
            WHERE DEVICE_ID = ? AND HEART_RATE > 0
            ORDER BY TIMESTAMP DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(deviceId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return HeartRateSample(bpm = 0, measuredAtEpochMillis = null)
            }
            return HeartRateSample(
                bpm = cursor.getInt(1),
                measuredAtEpochMillis = normalizeTimestamp(cursor.getLong(0)),
            )
        }
    }

    private fun readLatestBatteryStatus(db: SQLiteDatabase, deviceId: Long): BatteryStatus {
        db.rawQuery(
            """
            SELECT TIMESTAMP, LEVEL
            FROM BATTERY_LEVEL
            WHERE DEVICE_ID = ?
            ORDER BY TIMESTAMP DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(deviceId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return BatteryStatus(levelPercent = 0, measuredAtEpochMillis = null)
            }
            return BatteryStatus(
                levelPercent = cursor.getInt(1),
                measuredAtEpochMillis = normalizeTimestamp(cursor.getLong(0)),
            )
        }
    }

    private fun readLatestStress(db: SQLiteDatabase, deviceId: Long): StressSample {
        db.rawQuery(
            """
            SELECT TIMESTAMP, STRESS
            FROM XIAOMI_ACTIVITY_SAMPLE
            WHERE DEVICE_ID = ? AND STRESS > 0
            ORDER BY TIMESTAMP DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(deviceId.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return StressSample(level = 0, measuredAtEpochMillis = null)
            }
            return StressSample(
                level = cursor.getInt(1),
                measuredAtEpochMillis = normalizeTimestamp(cursor.getLong(0)),
            )
        }
    }

    private fun readLatestSleep(db: SQLiteDatabase, deviceId: Long): SleepSummary {
        val primarySleep = readSleepRow(
            db = db,
            deviceId = deviceId,
            minDurationMinutes = 120,
        ) ?: readSleepRow(
            db = db,
            deviceId = deviceId,
            minDurationMinutes = 1,
        )

        return if (primarySleep == null) {
            SleepSummary(
                totalMinutes = 0,
                deepSleepMinutes = 0,
                lightSleepMinutes = 0,
                remSleepMinutes = 0,
                awakeMinutes = 0,
                fellAsleepLabel = "--:--",
                wokeUpLabel = "--:--",
                updatedAtEpochMillis = null,
            )
        } else {
            val startMillis = normalizeTimestamp(primarySleep.startTimestamp)
            val wakeMillis = normalizeTimestamp(primarySleep.wakeupTimestamp)
            SleepSummary(
                totalMinutes = primarySleep.totalMinutes,
                deepSleepMinutes = primarySleep.deepSleepMinutes,
                lightSleepMinutes = primarySleep.lightSleepMinutes,
                remSleepMinutes = primarySleep.remSleepMinutes,
                awakeMinutes = primarySleep.awakeMinutes,
                fellAsleepLabel = formatClock(startMillis),
                wokeUpLabel = formatClock(wakeMillis),
                updatedAtEpochMillis = max(startMillis, wakeMillis),
            )
        }
    }

    private fun readSleepRow(
        db: SQLiteDatabase,
        deviceId: Long,
        minDurationMinutes: Int,
    ): SleepRow? {
        db.rawQuery(
            """
            SELECT TIMESTAMP, WAKEUP_TIME, TOTAL_DURATION, DEEP_SLEEP_DURATION, LIGHT_SLEEP_DURATION, REM_SLEEP_DURATION, AWAKE_DURATION
            FROM XIAOMI_SLEEP_TIME_SAMPLE
            WHERE DEVICE_ID = ? AND TOTAL_DURATION >= ?
            ORDER BY WAKEUP_TIME DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(deviceId.toString(), minDurationMinutes.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return SleepRow(
                startTimestamp = cursor.getLong(0),
                wakeupTimestamp = cursor.getLong(1),
                totalMinutes = cursor.getInt(2),
                deepSleepMinutes = cursor.getInt(3),
                lightSleepMinutes = cursor.getInt(4),
                remSleepMinutes = cursor.getInt(5),
                awakeMinutes = cursor.getInt(6),
            )
        }
    }

    private fun normalizeTimestamp(value: Long): Long {
        return if (value > 10_000_000_000L) value else value * 1000L
    }

    private fun formatClock(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    private data class DeviceInfo(
        val id: Long,
        val label: String,
        val name: String,
        val manufacturer: String,
        val model: String,
        val alias: String?,
        val typeName: String,
    )

    private data class DailySummary(
        val timestampMillis: Long? = null,
        val steps: Int = 0,
        val calories: Int = 0,
        val restingHeartRate: Int = 0,
        val averageHeartRate: Int = 0,
        val maxHeartRate: Int = 0,
        val maxHeartRateAtEpochMillis: Long? = null,
        val minHeartRate: Int = 0,
        val minHeartRateAtEpochMillis: Long? = null,
        val averageStress: Int = 0,
        val maxStress: Int = 0,
        val minStress: Int = 0,
        val averageSpo2: Int = 0,
        val maxSpo2: Int = 0,
        val maxSpo2AtEpochMillis: Long? = null,
        val minSpo2: Int = 0,
        val minSpo2AtEpochMillis: Long? = null,
        val vitalityCurrent: Int = 0,
    )

    private data class SleepRow(
        val startTimestamp: Long,
        val wakeupTimestamp: Long,
        val totalMinutes: Int,
        val deepSleepMinutes: Int,
        val lightSleepMinutes: Int,
        val remSleepMinutes: Int,
        val awakeMinutes: Int,
    )
}

private fun android.database.Cursor.longOrNull(index: Int): Long? {
    return if (isNull(index)) null else getLong(index)
}
