package dev.busung.s25uroot

import android.os.Build
import android.system.Os
import android.system.OsConstants
import java.io.File

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val kernelRelease: String,
    val kernelVersion: String,
    val buildId: String,
    val fingerprint: String,
    val androidRelease: String,
    val sdk: Int,
    val abi: String,
    val pageSize: Long,
) {
    val targetLabel: String
        get() = "$kernelRelease / $buildId"

    companion object {
        fun current() = DeviceSnapshot(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            kernelRelease = Os.uname().release,
            kernelVersion = File("/proc/version").readText(Charsets.US_ASCII).trim(),
            buildId = Build.DISPLAY,
            fingerprint = Build.FINGERPRINT,
            androidRelease = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            pageSize = Os.sysconf(OsConstants._SC_PAGESIZE),
        )
    }
}
