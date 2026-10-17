package dev.busung.s25uroot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Owns every AlarmManager interaction used by the boot auto-apply loop.
 *
 * The app declares USE_EXACT_ALARM, so the exact path is granted at install
 * and cannot be revoked by the user. The inexact fallback stays as a guard:
 * it is what runs if an OEM revokes the grant or the platform refuses the
 * exact call, and the choice is recorded so settings can tell the user that a
 * run may drift instead of silently missing it.
 */
object AutoRunScheduler {
    private const val TAG = "AutoRunScheduler"
    private const val REQUEST_CODE = 42

    /** Schedules a run [delaySeconds] from now. Returns false if the alarm was refused. */
    fun schedule(context: Context, delaySeconds: Int): Boolean =
        scheduleAt(context, SystemClock.elapsedRealtime() + delaySeconds.coerceAtLeast(1) * 1000L)

    fun cancel(context: Context) {
        val pendingIntent = pendingIntent(context, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            runCatching { alarmManager(context).cancel(pendingIntent) }
                .onFailure { Log.w(TAG, "Unable to cancel the auto-run alarm", it) }
            pendingIntent.cancel()
        }
        AutoRunManager.clearSchedule(context)
    }

    /**
     * Best-effort foreground launch for an already unlocked device. Failures
     * are ignored on purpose: the notification posted by
     * [AutoRunAlarmReceiver] is the reliable way in, and this only spares the
     * user a tap when the platform allows the launch.
     */
    fun openInstaller(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = BootReceiver.ACTION_AUTO_RUN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Log.d(TAG, "Foreground launch refused, waiting for the notification", it) }
    }

    private fun scheduleAt(context: Context, triggerAtElapsedMillis: Long): Boolean {
        val manager = alarmManager(context)
        val pendingIntent = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return false
        val exact = runCatching { manager.canScheduleExactAlarms() }.getOrDefault(false)
        val scheduled = runCatching {
            if (exact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtElapsedMillis,
                    pendingIntent,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtElapsedMillis,
                    pendingIntent,
                )
            }
            true
        }.getOrElse { error ->
            Log.e(TAG, "Unable to schedule the auto-run alarm", error)
            false
        }
        if (scheduled) {
            AutoRunManager.noteSchedule(context, triggerAtElapsedMillis, exact)
        }
        return scheduled
    }

    /**
     * Targets [AutoRunAlarmReceiver] rather than the launcher activity: a
     * broadcast is always delivered, while a background activity launch may be
     * dropped before the payload ever gets a chance to run.
     */
    private fun pendingIntent(context: Context, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AutoRunAlarmReceiver::class.java).apply {
                action = AutoRunAlarmReceiver.ACTION_AUTO_RUN_ALARM
            },
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}