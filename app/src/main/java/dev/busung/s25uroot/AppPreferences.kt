package dev.busung.s25uroot

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

enum class AccentColor(val storedValue: String) {
    Dynamic("dynamic"),
    Blue("blue"),
    Violet("violet"),
    Green("green"),
    Orange("orange");

    companion object {
        fun fromStoredValue(value: String?): AccentColor =
            entries.firstOrNull { it.storedValue == value } ?: Dynamic
    }
}

enum class AppThemeMode(val storedValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStoredValue(value: String?): AppThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: System
    }
}

object AppPreferences {
    private const val PREFERENCES = "appearance"
    private const val ACCENT_COLOR = "accent_color"
    private const val THEME_MODE = "theme_mode"
    private const val ADVANCED_MODE = "advanced_mode"
    private const val SHIZUKU_MODE = "shizuku_mode"
    private const val PAYLOAD_REPOSITORY = "payload_repository"
    private const val PAYLOAD_BRANCH = "payload_branch"
    private const val CONSUMED_INSTALL_REQUEST = "consumed_install_request"
    private const val AUTO_RUN_ON_BOOT = "auto_run_on_boot"
    private const val AUTO_RUN_MAX_ATTEMPTS = "auto_run_max_attempts"
    private const val BOOT_DELAY = "boot_delay_seconds"
    private const val PAYLOADS_SAVE_ENABLED = "payloads_save_enabled"

    const val DEFAULT_PAYLOAD_REPOSITORY = "BuSung-dev/Root-My-Galaxy-Payloads"
    const val DEFAULT_PAYLOAD_BRANCH = "main"
    const val DEFAULT_BOOT_DELAY = 60
    const val MIN_BOOT_DELAY = 20
    const val MAX_BOOT_DELAY = 120

    /** Offered as one-tap chips in the boot delay dialog. */
    val BOOT_DELAY_PRESETS = intArrayOf(20, 30, 50, 90, 120)

    fun payloadRepository(context: Context): String =
        prefs(context).getString(PAYLOAD_REPOSITORY, DEFAULT_PAYLOAD_REPOSITORY)
            ?: DEFAULT_PAYLOAD_REPOSITORY

    fun setPayloadRepository(context: Context, repository: String) {
        prefs(context).edit()
            .putString(PAYLOAD_REPOSITORY, repository)
            .apply()
    }

    fun payloadBranch(context: Context): String =
        prefs(context).getString(PAYLOAD_BRANCH, DEFAULT_PAYLOAD_BRANCH)
            ?: DEFAULT_PAYLOAD_BRANCH

    fun setPayloadBranch(context: Context, branch: String) {
        prefs(context).edit()
            .putString(PAYLOAD_BRANCH, branch)
            .apply()
    }

    fun accentColor(context: Context): AccentColor = AccentColor.fromStoredValue(
        prefs(context).getString(ACCENT_COLOR, null),
    )

    fun setAccentColor(context: Context, color: AccentColor) {
        prefs(context).edit()
            .putString(ACCENT_COLOR, color.storedValue)
            .apply()
    }

    fun themeMode(context: Context): AppThemeMode = AppThemeMode.fromStoredValue(
        prefs(context).getString(THEME_MODE, null),
    )

    fun setThemeMode(context: Context, themeMode: AppThemeMode) {
        prefs(context).edit()
            .putString(THEME_MODE, themeMode.storedValue)
            .apply()
    }

    fun advancedMode(context: Context): Boolean =
        prefs(context).getBoolean(ADVANCED_MODE, false)

    fun setAdvancedMode(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(ADVANCED_MODE, enabled)
            .apply()
    }

    fun shizukuMode(context: Context): Boolean =
        prefs(context).getBoolean(SHIZUKU_MODE, false)

    fun setShizukuMode(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(SHIZUKU_MODE, enabled)
            .apply()
    }

    fun autoRunOnBoot(context: Context): Boolean =
        prefs(context).getBoolean(AUTO_RUN_ON_BOOT, false)

    fun setAutoRunOnBoot(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(AUTO_RUN_ON_BOOT, enabled)
            .apply()
    }

    /** How many auto-apply attempts may run before the loop pauses itself. */
    fun autoRunMaxAttempts(context: Context): Int =
        prefs(context).getInt(AUTO_RUN_MAX_ATTEMPTS, AutoRunPolicy.DEFAULT_MAX_ATTEMPTS)

    fun setAutoRunMaxAttempts(context: Context, attempts: Int) {
        prefs(context).edit()
            .putInt(AUTO_RUN_MAX_ATTEMPTS, AutoRunPolicy.clampMaxAttempts(attempts))
            .apply()
    }

    fun bootDelay(context: Context): Int =
        prefs(context).getInt(BOOT_DELAY, DEFAULT_BOOT_DELAY)

    fun setBootDelay(context: Context, delaySeconds: Int) {
        prefs(context).edit()
            .putInt(BOOT_DELAY, delaySeconds.coerceIn(MIN_BOOT_DELAY, MAX_BOOT_DELAY))
            .apply()
    }

    fun payloadsSaveEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PAYLOADS_SAVE_ENABLED, false)

    fun setPayloadsSaveEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(PAYLOADS_SAVE_ENABLED, enabled)
            .apply()
    }

    @Synchronized
    fun consumeInstallRequest(context: Context, requestId: String?): Boolean {
        if (requestId.isNullOrBlank()) return false
        val preferences = prefs(context)
        if (preferences.getString(CONSUMED_INSTALL_REQUEST, null) == requestId) return false
        return preferences.edit()
            .putString(CONSUMED_INSTALL_REQUEST, requestId)
            .commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun languageTag(context: Context): String {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) "" else locales[0].toLanguageTag()
    }

    fun setLanguage(context: Context, languageTag: String) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(languageTag)
    }
}
