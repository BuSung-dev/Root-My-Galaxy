package dev.busung.s25uroot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that runs the standalone install path at boot.
 *
 * Forces Shizuku mode off for the duration of the run, so the exploit runs
 * directly in the app's own process. Artifacts are fetched through the
 * normal commit-pinned download flow, so this needs connectivity shortly
 * after boot.
 *
 * The service stops itself when the install reaches a terminal phase
 * (Installed / Failed). It is not restarted by the system afterwards;
 * re-running happens on the next boot or from the app UI.
 */
class BootInstallService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notificationId = 0x42554f54

    private var previousShizukuMode: Boolean? = null

    private lateinit var viewModel: InstallViewModel

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // Capture the user's preference so the boot run forces standalone mode
        // without permanently changing the setting. Shizuku is generally not
        // reachable at boot time.
        previousShizukuMode = AppPreferences.shizukuMode(this)
        AppPreferences.setShizukuMode(this, false)
        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(InstallViewModel::class.java)
        startForeground(
            notificationId,
            buildNotification(getString(R.string.status_checking_github)),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            viewModel.state.collectLatest { state ->
                val title = when (state.phase) {
                    InstallPhase.Exploiting -> getString(R.string.status_exploit_running)
                    InstallPhase.LoadingKernelSu -> getString(R.string.status_ksu_loading)
                    InstallPhase.Installed -> getString(R.string.status_ksu_active)
                    InstallPhase.Failed -> getString(R.string.status_install_failed)
                    else -> getString(R.string.status_checking_github)
                }
                val text = state.log.lineSequence().lastOrNull()?.take(120)
                    ?: state.message
                notify(title, text)
                if (state.phase == InstallPhase.Installed || state.phase == InstallPhase.Failed) {
                    stopSelf()
                }
            }
        }
        scope.launch {
            viewModel.install()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        previousShizukuMode?.let { AppPreferences.setShizukuMode(this, it) }
        scope.cancel()
        stopForegroundCompat()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notify(title: String, text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, buildNotification(title, text))
    }

    private fun buildNotification(title: String, text: String = "") =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(launcherPendingIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun launcherPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_boot),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "boot_install"

        fun start(context: Context) {
            val intent = Intent(context, BootInstallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
