package app.lhx.mibandmcp.model

import kotlinx.serialization.Serializable

@Serializable
data class EndpointInfo(
    val host: String = "",
    val port: Int = 0,
    val url: String = "",
)

@Serializable
data class ServiceStatus(
    val isRunning: Boolean = false,
    val endpoint: EndpointInfo? = null,
    val message: String? = null,
)

@Serializable
data class BandStatus(
    val gadgetbridgeInstalled: Boolean = false,
    val exportGranted: Boolean = false,
    val dataReady: Boolean = false,
    val detail: String = "",
)

@Serializable
data class DeviceProfile(
    val name: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val alias: String? = null,
    val typeName: String = "",
    val firmwareVersion: String? = null,
)

@Serializable
data class ActivitySummary(
    val stepsToday: Int = 0,
)

@Serializable
data class DailyMetrics(
    val summaryEpochMillis: Long? = null,
    val caloriesToday: Int = 0,
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

@Serializable
data class HeartRateSample(
    val bpm: Int = 0,
    val measuredAtEpochMillis: Long? = null,
)

@Serializable
data class BatteryStatus(
    val levelPercent: Int = 0,
    val measuredAtEpochMillis: Long? = null,
)

@Serializable
data class StressSample(
    val level: Int = 0,
    val measuredAtEpochMillis: Long? = null,
)

@Serializable
data class SleepSummary(
    val totalMinutes: Int = 0,
    val deepSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val remSleepMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val fellAsleepLabel: String = "--:--",
    val wokeUpLabel: String = "--:--",
    val updatedAtEpochMillis: Long? = null,
)

@Serializable
data class SyncStatus(
    val isRefreshing: Boolean = false,
    val lastSyncEpochMillis: Long? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

@Serializable
data class AppSnapshot(
    val serviceStatus: ServiceStatus = ServiceStatus(),
    val bandStatus: BandStatus = BandStatus(),
    val deviceProfile: DeviceProfile = DeviceProfile(),
    val activitySummary: ActivitySummary = ActivitySummary(),
    val dailyMetrics: DailyMetrics = DailyMetrics(),
    val heartRateSample: HeartRateSample = HeartRateSample(),
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val stressSample: StressSample = StressSample(),
    val sleepSummary: SleepSummary = SleepSummary(),
    val syncStatus: SyncStatus = SyncStatus(),
)
