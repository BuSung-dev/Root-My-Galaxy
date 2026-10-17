package dev.busung.s25uroot

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/** How the most recent auto-apply attempt ended. */
enum class AutoRunOutcome {
    Succeeded,
    Failed,
    Interrupted,
}

/** Why the loop paused itself. Shown in settings so the pause is never silent. */
enum class AutoRunSuspendReason {
    /** The attempt budget for this loop ran out. */
    AttemptsExhausted,

    /** The exploit rebooted the device, so retrying risked a bootloop. */
    Bootloop,
}

enum class AutoRunAction {
    Proceed,
    Retry,
    Suspend,
}

data class AutoRunAttempt(
    val attempt: Int,
    val maxAttempts: Int,
    val action: AutoRunAction,
    val retryInSeconds: Int = 0,
) {
    val retriesLeft: Int get() = (maxAttempts - attempt).coerceAtLeast(0)
}

data class AutoRunStatus(
    val enabled: Boolean = false,
    val suspended: AutoRunSuspendReason? = null,
    val attempts: Int = 0,
    val maxAttempts: Int = AutoRunPolicy.DEFAULT_MAX_ATTEMPTS,
    val consecutiveFailures: Int = 0,
    val lastAttemptMillis: Long? = null,
    val lastOutcome: AutoRunOutcome? = null,
    val inProgress: Boolean = false,
    val nextTriggerElapsedMillis: Long? = null,
    val nextTriggerExact: Boolean = false,
    val awaitingUnlock: Boolean = false,
) {
    val attemptsLeft: Int get() = (maxAttempts - attempts).coerceAtLeast(0)
    val scheduled: Boolean get() = nextTriggerElapsedMillis != null
}

/**
 * Owns the state machine behind "auto-run on boot".
 *
 * Every attempt runs the exploit, and the exploit may reboot the phone. The
 * counter therefore survives reboots: it counts consecutive attempts across
 * boots and is only cleared by a success or by the user disarming the toggle.
 * That keeps the loop bounded without ever leaving a stale "running" flag
 * behind that would make the UI claim an exploit is in progress forever.
 */
object AutoRunManager {
    private const val PREFS = "auto_run"
    private const val BOOT_ID_BEFORE_KEY = "boot_id_before_exploit"
    private const val ATTEMPTS_KEY = "boot_attempt_counter"
    private const val ATTEMPT_STARTED_KEY = "attempt_started_millis"
    private const val LAST_ATTEMPT_KEY = "last_boot_attempt_millis"
    private const val LAST_OUTCOME_KEY = "last_outcome"
    private const val FAILURES_KEY = "consecutive_failures"
    private const val TRIGGERED_KEY = "auto_run_triggered"
    private const val SUSPEND_REASON_KEY = "suspend_reason"
    private const val NEXT_TRIGGER_KEY = "next_trigger_elapsed_millis"
    private const val NEXT_TRIGGER_EXACT_KEY = "next_trigger_exact"
    private const val AWAITING_UNLOCK_KEY = "awaiting_unlock"

    /**
     * An attempt older than this is treated as dead. The exploit is given a
     * hard 15 minute deadline, so anything past this window was interrupted
     * by the process being killed rather than still running.
     */
    private const val ATTEMPT_WINDOW_MILLIS = 30 * 60_000L

    fun status(context: Context): AutoRunStatus {
        val preferences = prefs(context)
        val attemptStartedAt = preferences.getLong(ATTEMPT_STARTED_KEY, 0L)
        return AutoRunStatus(
            enabled = AppPreferences.autoRunOnBoot(context),
            suspended = readEnum(preferences, SUSPEND_REASON_KEY),
            attempts = preferences.getInt(ATTEMPTS_KEY, 0),
            maxAttempts = AppPreferences.autoRunMaxAttempts(context),
            consecutiveFailures = preferences.getInt(FAILURES_KEY, 0),
            lastAttemptMillis = preferences.getLong(LAST_ATTEMPT_KEY, 0L).takeIf { it > 0L },
            lastOutcome = readEnum(preferences, LAST_OUTCOME_KEY),
            inProgress = preferences.getBoolean(TRIGGERED_KEY, false) &&
                attemptStartedAt > 0L &&
                System.currentTimeMillis() - attemptStartedAt < ATTEMPT_WINDOW_MILLIS,
            nextTriggerElapsedMillis = preferences.getLong(NEXT_TRIGGER_KEY, 0L).takeIf { it > 0L },
            nextTriggerExact = preferences.getBoolean(NEXT_TRIGGER_EXACT_KEY, false),
            awaitingUnlock = preferences.getBoolean(AWAITING_UNLOCK_KEY, false),
        )
    }

    /**
     * Arm the next attempt. Callers must not start the exploit unless this
     * returns [AutoRunAction.Proceed]; the returned [AutoRunAction.Retry]
     * carries the backoff wait the scheduler should use instead.
     */
    fun beginAttempt(context: Context): AutoRunAttempt {
        val preferences = prefs(context)
        val maxAttempts = AppPreferences.autoRunMaxAttempts(context)
        val suspended = readEnum<AutoRunSuspendReason>(preferences, SUSPEND_REASON_KEY)
        if (suspended != null) {
            return AutoRunAttempt(
                attempt = preferences.getInt(ATTEMPTS_KEY, 0),
                maxAttempts = maxAttempts,
                action = AutoRunAction.Suspend,
            )
        }
        // The boot id is stored as the failsafe token: if it changes later the
        // exploit rebooted the device instead of completing.
        val bootToken = currentBootToken()
        // A token from a previous boot means the exploit rebooted the phone
        // instead of completing. The attempt is already counted, so this only
        // decides how the loop reports and eventually pauses.
        val rebooted = bootToken != null &&
            preferences.getString(BOOT_ID_BEFORE_KEY, null)?.let { it != bootToken } == true
        val attempt = preferences.getInt(ATTEMPTS_KEY, 0) + 1
        if (attempt > maxAttempts) {
            suspend(
                context,
                if (rebooted) AutoRunSuspendReason.Bootloop else AutoRunSuspendReason.AttemptsExhausted,
            )
            return AutoRunAttempt(attempt, maxAttempts, AutoRunAction.Suspend)
        }
        val now = System.currentTimeMillis()
        val editor = preferences.edit()
            .putInt(ATTEMPTS_KEY, attempt)
            .putString(BOOT_ID_BEFORE_KEY, bootToken)
            .putBoolean(TRIGGERED_KEY, true)
            .putLong(ATTEMPT_STARTED_KEY, now)
            .putLong(LAST_ATTEMPT_KEY, now)
            .remove(NEXT_TRIGGER_KEY)
            .putBoolean(AWAITING_UNLOCK_KEY, false)
        if (rebooted) {
            editor.putString(LAST_OUTCOME_KEY, AutoRunOutcome.Interrupted.name)
        }
        editor.apply()
        return AutoRunAttempt(attempt, maxAttempts, AutoRunAction.Proceed)
    }

    /** Clears the loop after a verified install, so the next boot starts over. */
    fun recordSuccess(context: Context) {
        prefs(context).edit()
            .putInt(ATTEMPTS_KEY, 0)
            .putInt(FAILURES_KEY, 0)
            .putString(LAST_OUTCOME_KEY, AutoRunOutcome.Succeeded.name)
            .remove(BOOT_ID_BEFORE_KEY)
            .remove(TRIGGERED_KEY)
            .remove(ATTEMPT_STARTED_KEY)
            .remove(SUSPEND_REASON_KEY)
            .remove(NEXT_TRIGGER_KEY)
            .apply()
    }

    /**
     * Clears the loop after a failed attempt. Returns [AutoRunAction.Retry]
     * with the backoff wait while attempts remain, or [AutoRunAction.Suspend]
     * once the budget is spent or the exploit rebooted the device.
     */
    fun recordFailure(context: Context, interrupted: Boolean): AutoRunAttempt {
        val preferences = prefs(context)
        val maxAttempts = AppPreferences.autoRunMaxAttempts(context)
        val attempts = preferences.getInt(ATTEMPTS_KEY, 0).coerceAtLeast(1)
        val failures = preferences.getInt(FAILURES_KEY, 0) + 1
        preferences.edit()
            .putInt(FAILURES_KEY, failures)
            .putString(
                LAST_OUTCOME_KEY,
                (if (interrupted) AutoRunOutcome.Interrupted else AutoRunOutcome.Failed).name,
            )
            .remove(BOOT_ID_BEFORE_KEY)
            .remove(TRIGGERED_KEY)
            .remove(ATTEMPT_STARTED_KEY)
            .apply()

        val stillArmed = AppPreferences.autoRunOnBoot(context) &&
            readEnum<AutoRunSuspendReason>(preferences, SUSPEND_REASON_KEY) == null
        if (!stillArmed || !AutoRunPolicy.shouldRetry(attempts, maxAttempts)) {
            val reason = if (interrupted) {
                AutoRunSuspendReason.Bootloop
            } else {
                AutoRunSuspendReason.AttemptsExhausted
            }
            suspend(context, reason)
            return AutoRunAttempt(attempts, maxAttempts, AutoRunAction.Suspend)
        }
        return AutoRunAttempt(
            attempt = attempts,
            maxAttempts = maxAttempts,
            action = AutoRunAction.Retry,
            retryInSeconds = retryDelaySeconds(context, attempts),
        )
    }

    /**
     * True when the exploit rebooted the device mid-attempt, which means the
     * payload never completed. Consumes the failsafe token so it is reported
     * exactly once.
     */
    fun detectUnexpectedReboot(context: Context): Boolean {
        val preferences = prefs(context)
        val savedBootId = preferences.getString(BOOT_ID_BEFORE_KEY, null) ?: return false
        if (savedBootId == currentBootToken()) return false
        preferences.edit().remove(BOOT_ID_BEFORE_KEY).apply()
        return true
    }

    fun isAttemptInProgress(context: Context): Boolean = status(context).inProgress

    fun isSuspended(context: Context): Boolean = status(context).suspended != null

    /** The user armed the toggle: clear every counter and un-pause the loop. */
    fun onAutoRunEnabled(context: Context) {
        prefs(context).edit()
            .putInt(ATTEMPTS_KEY, 0)
            .putInt(FAILURES_KEY, 0)
            .remove(SUSPEND_REASON_KEY)
            .remove(BOOT_ID_BEFORE_KEY)
            .remove(TRIGGERED_KEY)
            .remove(ATTEMPT_STARTED_KEY)
            .remove(AWAITING_UNLOCK_KEY)
            .apply()
    }

    /** The user disarmed the toggle: drop the failsafe state, keeping history. */
    fun onAutoRunDisabled(context: Context) {
        prefs(context).edit()
            .putInt(ATTEMPTS_KEY, 0)
            .remove(SUSPEND_REASON_KEY)
            .remove(BOOT_ID_BEFORE_KEY)
            .remove(TRIGGERED_KEY)
            .remove(ATTEMPT_STARTED_KEY)
            .remove(AWAITING_UNLOCK_KEY)
            .apply()
    }

    /**
     * Drops the pending reboot token once a run is known to have completed,
     * so a later launch is not read as an interrupted attempt.
     */
    fun clearFailsafe(context: Context) {
        prefs(context).edit()
            .remove(BOOT_ID_BEFORE_KEY)
            .remove(TRIGGERED_KEY)
            .remove(ATTEMPT_STARTED_KEY)
            .apply()
    }

    fun noteSchedule(context: Context, triggerElapsedMillis: Long, exact: Boolean) {
        prefs(context).edit()
            .putLong(NEXT_TRIGGER_KEY, triggerElapsedMillis)
            .putBoolean(NEXT_TRIGGER_EXACT_KEY, exact)
            .apply()
    }

    fun clearSchedule(context: Context) {
        prefs(context).edit()
            .remove(NEXT_TRIGGER_KEY)
            .remove(AWAITING_UNLOCK_KEY)
            .apply()
    }

    /**
     * The alarm fired but the phone is still locked, so the run is still
     * pending. Only informational: [AutoRunAlarmReceiver] re-arms the alarm,
     * so there is nothing to consume and no state to clear on unlock.
     */
    fun markAwaitingUnlock(context: Context) {
        prefs(context).edit().putBoolean(AWAITING_UNLOCK_KEY, true).apply()
    }

    fun clearAwaitingUnlock(context: Context) {
        prefs(context).edit().putBoolean(AWAITING_UNLOCK_KEY, false).apply()
    }

    /** Wall-clock time of the pending alarm, for display purposes only. */
    fun nextRunWallClockMillis(context: Context): Long? = autoRunNextRunWallClock(status(context))

    private fun retryDelaySeconds(context: Context, attempts: Int): Int =
        AutoRunPolicy.retryDelaySeconds(AppPreferences.bootDelay(context), attempts)

    private fun suspend(context: Context, reason: AutoRunSuspendReason) {
        prefs(context).edit()
            .putString(SUSPEND_REASON_KEY, reason.name)
            .remove(NEXT_TRIGGER_KEY)
            .apply()
        AppPreferences.setAutoRunOnBoot(context, false)
    }

    private inline fun <reified T : Enum<T>> readEnum(
        preferences: SharedPreferences,
        key: String,
    ): T? {
        val name = preferences.getString(key, null) ?: return null
        return runCatching { enumValueOf<T>(name) }.getOrNull()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun currentBootToken(): String? = runCatching {
        File("/proc/sys/kernel/random/boot_id")
            .readText()
            .trim()
            .takeIf { it.isNotBlank() }
    }.getOrNull()
}