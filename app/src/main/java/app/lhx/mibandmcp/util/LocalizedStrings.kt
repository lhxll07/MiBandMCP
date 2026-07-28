package app.lhx.mibandmcp.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

fun Context.localizedString(@StringRes resourceId: Int, vararg formatArgs: Any): String =
    ContextCompat.getContextForLanguage(this).getString(resourceId, *formatArgs)
