package dev.busung.s25uroot

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Alarm target for the boot auto-apply run.
 *
 * This used to point straight at [MainActivity], which meant starting an
 * activity from the background: blocked on Android 10+ unless an exemption
 * applies, and unreliable even when one does. The exploit also only applies
 * with the app in the foreground, so the run is announced instead of forced,
 * and the notification hands control to the activity.
 *
 * The unlock decision lives here rather than in a broadcast receiver. One UI
 * can take over a minute to deliver BOOT_COMPLETED, by which time
 * USER_PRESENT has already fired and been discarded, and this build drops
 * implicit broadcasts aimed at manifest receivers anyway. Re-arming from the
 * alarm itself makes the wait self-healing: if the phone is still locked the
 * run simply waits for the next tick instead of depending on an event that
 * may never arrive again.
 */
class AutoRunAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTO_RUN_ALARM) return
        if (!AppPreferences.autoRunOnBoot(context)) return

        val status = AutoRunManager.status(context)
        if (status.suspended != null) {
            Log.d(TAG, "Auto-run is paused, dropping the alarm")
            return
        }

        val delaySeconds = AppPreferences.bootDelay(context)
        if (isDeviceLocked(context)) {
            // Still locked: keep waiting without spending an attempt, and
            // re-arm so the prompt appears as soon as the phone is used.
            AutoRunManager.markAwaitingUnlock(context)
            AutoRunScheduler.schedule(context, delaySeconds)
            Log.d(TAG, "Still locked, auto-run re-armed in ${delaySeconds}s")
            return
        }

        AutoRunManager.clearAwaitingUnlock(context)
        val attempt = status.attempts + 1
        val posted = AutoRunNotifier.notifyRunReady(context, attempt, status.maxAttempts)
        Log.d(
            TAG,
            "Auto-run due (attempt $attempt/${status.maxAttempts}), " +
                "notified=$posted canPost=${AutoRunNotifier.canPost(context)} " +
                "channelEnabled=${AutoRunNotifier.isChannelEnabled(context)}",
        )

        // The notification is the reliable path. When it fails, a direct
        // launch may still get through if the phone is in the user's hands.
        if (!posted) {
            AutoRunScheduler.openInstaller(context)
        }
    }

    private fun isDeviceLocked(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        // A device without a keyguard service reports locked=false, which is
        // exactly the case where running straight away is safe.
        return keyguard?.isDeviceLocked != false
    }

    companion object {
        const val ACTION_AUTO_RUN_ALARM = "dev.busung.s25uroot.AUTO_RUN_ALARM"
        private const val TAG = "AutoRunAlarm"
    }
}