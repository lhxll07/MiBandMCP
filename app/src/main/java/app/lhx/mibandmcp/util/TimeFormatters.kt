package app.lhx.mibandmcp.util

import android.content.Context
import app.lhx.mibandmcp.R
import java.util.concurrent.TimeUnit

object TimeFormatters {
    fun relativeTime(context: Context, epochMillis: Long?): String {
        epochMillis ?: return context.localizedString(R.string.relative_time_unknown)
        val delta = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        return when {
            minutes < 1 -> context.localizedString(R.string.relative_time_just_now)
            minutes < 60 -> context.localizedString(R.string.relative_time_minutes_ago, minutes)
            minutes < 24 * 60 -> context.localizedString(R.string.relative_time_hours_ago, minutes / 60)
            else -> context.localizedString(R.string.relative_time_days_ago, minutes / (24 * 60))
        }
    }

    fun sleepDuration(context: Context, totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return context.localizedString(R.string.sleep_duration_format, hours, minutes)
    }
}
