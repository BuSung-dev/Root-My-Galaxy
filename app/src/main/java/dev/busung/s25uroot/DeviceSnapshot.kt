package dev.busung.s25uroot

import android.os.Build
import android.system.Os
import android.system.OsConstants

data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val kernelRelease: String,
    val kernelVersionInfo: String,
    val machine: String,
    val buildId: String,
    val fingerprint: String,
    val androidRelease: String,
    val sdk: Int,
    val abi: String,
    val pageSize: Long,
) {
    val kernelVersion: String
        get() = kernelRelease.takeWhile { it.isDigit() || it == '.' }

    val kernelVersionFull: String
        get() = listOf(kernelRelease, kernelVersionInfo, machine)
            .filter(String::isNotBlank)
            .joinToString(" ")

    companion object {
        fun current(): DeviceSnapshot {
            val uname = Os.uname()
            return DeviceSnapshot(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                device = Build.DEVICE,
                kernelRelease = uname.release,
                kernelVersionInfo = uname.version,
                machine = uname.machine,
                buildId = Build.DISPLAY,
                fingerprint = Build.FINGERPRINT,
                androidRelease = Build.VERSION.RELEASE,
                sdk = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                pageSize = Os.sysconf(OsConstants._SC_PAGESIZE),
            )
        }
    }
}
