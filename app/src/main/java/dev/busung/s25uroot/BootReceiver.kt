package dev.busung.s25uroot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Boot-time trigger for the standalone (non-Shizuku) install path.
 *
 * Runs the exploit in the app's own process at boot — no Shizuku, no adb,
 * no network — matching the proven "run adb shell early at boot" window.
 * The payload itself waits for the boot allocator quiet window
 * (APP_MIN_BOOT_UPTIME_SEC), so firing at BOOT_COMPLETED lands inside it.
 *
 * Skipped when KernelSU is already live for this boot (root is volatile and
 * cleared on reboot, so a fresh boot normally needs a fresh run).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            !NativeProbe.isKernelSuActive()
        ) {
            BootInstallService.start(context)
        }
    }
}