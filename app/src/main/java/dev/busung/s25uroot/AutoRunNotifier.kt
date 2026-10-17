package dev.busung.s25uroot

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Announces that a boot attempt is due.
 *
 * The exploit only applies correctly with the app in the foreground, so
 * auto-run cannot simply start the installer from the background: Android 10+
 * blocks background activity launches, and even the alarm exemption is not
 * guaranteed. The notification is the supported way in, and tapping it is what
 * gives the run the foreground it needs.
 */
object AutoRunNotifier {
    private const val CHANNEL_ID = "auto_run"
    private const val NOTIFICATION_ID = 4101
    private const val REQUEST_CODE = 43
    private const val TAG = "AutoRunNotifier"

    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Posts the "your payload is ready" prompt.
     *
     * Every failure path returns false instead of throwing, because this runs
     * inside a BroadcastReceiver where a crash is invisible: the run would
     * silently never appear. The return value is what settings and the log
     * report back to the user.
     */
    fun notifyRunReady(context: Context, attempt: Int, maxAttempts: Int): Boolean {
        if (!canPost(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted, skipping the auto-run prompt")
            return false
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: run {
            Log.w(TAG, "NotificationManager unavailable")
            return false
        }
        ensureChannel(context, manager)
        return try {
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_auto_run)
                .setContentTitle(context.getString(R.string.auto_run_notify_title))
                .setContentText(context.getString(R.string.auto_run_notify_text, attempt, maxAttempts))
                .setContentIntent(contentIntent(context))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_EVENT)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (error: Throwable) {
            Log.e(TAG, "Unable to post the auto-run notification", error)
            false
        }
    }

    /** Whether the channel still allows a visible prompt, for the status card. */
    fun isChannelEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(CHANNEL_ID) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    /**
     * Opens the app straight into the boot attempt. The receiver hands control
     * to [MainActivity] and clears the action so the run cannot be replayed by
     * a configuration change.
     */
    private fun contentIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            action = BootReceiver.ACTION_AUTO_RUN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.auto_run_channel_name),
            // High importance keeps the heads-up banner, which is the whole
            // point: the run waits for the user to act on it.
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.auto_run_channel_description)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }
}