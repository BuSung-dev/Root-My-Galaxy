package dev.busung.s25uroot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-establishes root after a reboot: on these targets KernelSU is loaded as
 * a runtime module and nothing persists across power cycles, so the install
 * has to run again every boot. Skips when the module is already active.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (NativeProbe.isKernelSuActive()) return
        if (!AppPreferences.bootRootMode(context)) return
        BootInstallService.start(context)
    }
}
