package app.lhx.mibandmcp.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val languageTag: String?) {
    System(null),
    English("en"),
    SimplifiedChinese("zh-CN"),
    ;

    fun apply() {
        val locales = languageTag
            ?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    companion object {
        fun current(): AppLanguage {
            val languageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { language ->
                language.languageTag != null && languageTags.startsWith(language.languageTag)
            } ?: System
        }
    }
}
