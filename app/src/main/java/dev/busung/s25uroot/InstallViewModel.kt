package dev.busung.s25uroot

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

enum class InstallPhase {
    Checking,
    Ready,
    Downloading,
    Exploiting,
    LoadingKernelSu,
    Installed,
    Failed,
}

enum class InstallStability {
    Unknown,
    Stable,
    Unstable,
}

data class InstallUiState(
    val phase: InstallPhase = InstallPhase.Checking,
    val message: String = "",
    val probeOutput: String = "",
    val log: String = "",
    val stability: InstallStability = InstallStability.Unknown,
) {
    val busy: Boolean
        get() = phase in setOf(
            InstallPhase.Checking,
            InstallPhase.Downloading,
            InstallPhase.Exploiting,
            InstallPhase.LoadingKernelSu,
        )

    val systemStable: Boolean
        get() = stability == InstallStability.Stable
}

data class TargetCatalogUiState(
    val loading: Boolean = false,
    val profiles: List<TargetProfile> = emptyList(),
    val error: String? = null,
)

private data class CommandResult(val code: Int, val output: String)

/**
 * Payloads are truncated to a fixed release size, so a rebuild of a target --
 * or a different target padded to the same size -- has exactly the length of
 * whatever is already staged, and would keep running in its place.
 */
internal fun stagedFileIsCurrent(staged: File, source: File): Boolean {
    if (!staged.exists()) return false
    val stagedDigest = sha256OrNull(staged) ?: return false
    return stagedDigest == sha256OrNull(source)
}

private fun sha256OrNull(file: File): String? = runCatching {
    file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}.getOrNull()

class InstallViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repository = PayloadRepository(application)
    private val historyStore = InstallHistoryStore(application)
    private val mutableState = MutableStateFlow(InstallUiState())
    private val mutableHistory = MutableStateFlow(historyStore.closeInterruptedRuns())
    private val mutableTargetCatalog = MutableStateFlow(TargetCatalogUiState())
    private var discoveryJob: Job? = null
    private var installJob: Job? = null
    private var activeHistoryEntry: InstallHistoryEntry? = null

    @Volatile
    private var activeRunShizuku: Boolean? = null
    val state: StateFlow<InstallUiState> = mutableState.asStateFlow()
    val history: StateFlow<List<InstallHistoryEntry>> = mutableHistory.asStateFlow()
    val targetCatalog: StateFlow<TargetCatalogUiState> = mutableTargetCatalog.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (installJob?.isActive == true) return
        mutableHistory.value = historyStore.load()
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch(Dispatchers.IO) {
            val nativeAvailable = NativeProbe.isAvailable()
            val probe = if (nativeAvailable) runCatching { NativeProbe.run() }.getOrNull() ?: "" else ""
            val installed = if (nativeAvailable) runCatching { detectInstalled() }.getOrNull() ?: false else false
            if (installed) {
                val systemStable = runCatching { isSystemUiAlive() }.getOrNull() ?: true
                // Root is live, so any pending reboot token is stale. Leaving
                // it behind would make the next launch read as an interrupted
                // attempt once root stops being detected.
                if (systemStable) {
                    AutoRunManager.recordSuccess(app)
                } else {
                    AutoRunManager.clearFailsafe(app)
                }
                mutableState.value = InstallUiState(
                    phase = InstallPhase.Installed,
                    message = if (systemStable) app.getString(R.string.status_ksu_active) else app.getString(R.string.status_system_unstable),
                    probeOutput = probe,
                    log = probe,
                    stability = if (systemStable) InstallStability.Stable else InstallStability.Unstable,
                )
                return@launch
            }
            // The exploit reboots the device instead of completing when a payload
            // destabilises it. Report that as a failed attempt so the loop either
            // retries with a longer wait or pauses instead of bootlooping.
            val interrupted = runCatching { AutoRunManager.detectUnexpectedReboot(app) }.getOrNull() == true
            if (interrupted) {
                val decision = AutoRunManager.recordFailure(app, interrupted = true)
                if (decision.action == AutoRunAction.Retry) {
                    AutoRunScheduler.schedule(app, decision.retryInSeconds)
                }
                val profile = try { repository.resolveTarget(DeviceSnapshot.current()) } catch (_: Throwable) { null }
                val interruptedLog = buildString {
                    append(probe)
                    if (profile != null) append("\n${app.getString(R.string.log_profile, profile.profileId)}")
                    append("\n${app.getString(R.string.log_exploit_interrupted)}")
                    if (decision.action == AutoRunAction.Retry) {
                        append("\n${app.getString(R.string.auto_run_retry_scheduled, decision.retryInSeconds)}")
                    } else {
                        append("\n${app.getString(R.string.auto_run_suspended)}")
                    }
                }
                mutableState.value = InstallUiState(
                    phase = InstallPhase.Failed,
                    message = app.getString(R.string.status_install_failed),
                    probeOutput = probe,
                    log = interruptedLog,
                    stability = InstallStability.Unknown,
                )
                return@launch
            }
            // Check if an exploit is actively running (e.g., auto-run triggered on boot)
            val running = runCatching { isExploitRunning() }.getOrNull() ?: false
            if (running) {
                val profile = try { repository.resolveTarget(DeviceSnapshot.current()) } catch (_: Throwable) { null }
                val runningLog = buildString {
                    append(probe)
                    if (profile != null) append("\n${app.getString(R.string.log_profile, profile.profileId)}")
                    append("\n[*] ${app.getString(R.string.status_exploit_running)}")
                }
                mutableState.value = InstallUiState(
                    phase = InstallPhase.Exploiting,
                    message = app.getString(R.string.status_exploit_running),
                    probeOutput = probe,
                    log = runningLog,
                    stability = InstallStability.Unknown,
                )
                return@launch
            }
            try {
                val profile = repository.resolveTarget(DeviceSnapshot.current())
                mutableState.value = InstallUiState(
                    phase = InstallPhase.Ready,
                    message = app.getString(R.string.status_not_installed),
                    probeOutput = probe,
                    log = "$probe\n${app.getString(R.string.log_profile, profile.profileId)}",
                )
            } catch (error: Throwable) {
                mutableState.value = InstallUiState(
                    phase = InstallPhase.Failed,
                    message = app.getString(R.string.status_support_failed),
                    probeOutput = probe,
                    log = "$probe\n[-] ${error.message ?: error.javaClass.simpleName}",
                )
            }
        }
    }

    fun deleteHistoryEntries(ids: Collection<String>) {
        val runningId = activeHistoryEntry?.id
        val toDelete = ids.filterNot { it == runningId }
        if (toDelete.isEmpty()) return
        toDelete.forEach(historyStore::delete)
        mutableHistory.value = mutableHistory.value.filterNot { it.id in toDelete }
    }

    fun loadTargetCatalog() {
        if (mutableTargetCatalog.value.loading) return
        viewModelScope.launch(Dispatchers.IO) {
            mutableTargetCatalog.value = TargetCatalogUiState(loading = true)
            mutableTargetCatalog.value = try {
                TargetCatalogUiState(
                    profiles = repository.loadTargets().sortedWith(
                        compareBy(
                            TargetProfile::displayName,
                            TargetProfile::profileId,
                        ),
                    ),
                )
            } catch (error: Throwable) {
                TargetCatalogUiState(error = error.message ?: error.javaClass.simpleName)
            }
        }
    }

    fun install(profileId: String? = null) {
        if (installJob?.isActive == true || mutableState.value.phase == InstallPhase.Installed) return
        discoveryJob?.cancel()
        installJob = viewModelScope.launch(Dispatchers.IO) {
            mutableState.value = InstallUiState(
                phase = InstallPhase.Checking,
                probeOutput = mutableState.value.probeOutput,
            )
            startHistory()
            // Freeze the transport for the whole run so a mid-run preference
            // change cannot mix Shizuku and standalone execution between the
            // exploit and the KernelSU staging steps.
            activeRunShizuku = AppPreferences.shizukuMode(app)
            // Only an armed boot attempt feeds the retry loop; a manual run
            // from the installer must not burn the boot budget.
            val autoRunAttempt = AutoRunManager.isAttemptInProgress(app)
            try {
                // The boot_id failsafe token was armed by handleAutoRunIntent
                // before this activity started. Do NOT save it again here.
                if (shizukuEnabled()) {
                    appendLog(app.getString(R.string.log_shizuku_prepare))
                    if (!ShizukuController.isRunning() && !ShizukuController.pingUntilRunning()) {
                        error(app.getString(R.string.error_shizuku_unavailable))
                    }
                    if (!ShizukuController.isGranted() && !ShizukuController.requestPermission()) {
                        error(app.getString(R.string.error_shizuku_permission))
                    }
                    appendLog(app.getString(R.string.log_shizuku_permission))
                }
                setPhase(InstallPhase.Checking, app.getString(R.string.status_checking_github))
                val profile = if (profileId == null) {
                    repository.resolveTarget(DeviceSnapshot.current())
                } else {
                    repository.resolveTarget(profileId)
                }
                appendLog(app.getString(R.string.log_profile, profile.profileId))
                updateHistoryProfile(profile.profileId)

                // Check for cached payloads before downloading
                val hasCachedPayloads = repository.payloadsExistLocally(profile.profileId)
                if (hasCachedPayloads) {
                    appendLog(app.getString(R.string.payloads_cached))
                    val cachedPayloads = repository.getLocalPayloads(profile.profileId)
                    if (cachedPayloads != null) {
                        // Use cached payloads
                        setPhase(InstallPhase.Exploiting, app.getString(R.string.status_exploit_running))
                        executeExploit(cachedPayloads.exploit)
                        setPhase(InstallPhase.LoadingKernelSu, app.getString(R.string.status_ksu_loading))
                        installKernelSu(cachedPayloads)
                        setPhase(InstallPhase.Installed, app.getString(R.string.status_ksu_active))
                        appendLog(app.getString(R.string.log_install_complete))
                        finishHistory(InstallRunResult.Succeeded)
                        onInstallSucceeded()
                        return@launch
                    }
                }

                setPhase(InstallPhase.Downloading, app.getString(R.string.status_downloading_payload))
                val payloads = repository.download(profile) { appendLog("[*] $it") }
                appendLog(app.getString(R.string.log_download_verified))

                setPhase(InstallPhase.Exploiting, app.getString(R.string.status_exploit_running))
                executeExploit(payloads.exploit)

                setPhase(InstallPhase.LoadingKernelSu, app.getString(R.string.status_ksu_loading))
                installKernelSu(payloads)

                setPhase(InstallPhase.Installed, app.getString(R.string.status_ksu_active))
                appendLog(app.getString(R.string.log_install_complete))
                finishHistory(InstallRunResult.Succeeded)
                onInstallSucceeded()
            } catch (error: Throwable) {
                appendLog("[-] ${error.message ?: error.javaClass.simpleName}")
                setPhase(InstallPhase.Failed, app.getString(R.string.status_install_failed))
                finishHistory(InstallRunResult.Failed)
                if (autoRunAttempt) {
                    onAutoRunAttemptFailed()
                }
            } finally {
                activeRunShizuku = null
            }
        }
    }

    /**
     * A verified install resets the boot retry loop and re-arms the toggle for
     * the next boot. Auto-run is only re-armed when root is confirmed active
     * AND the system passed the stability check: a payload that broke SystemUI
     * must not be replayed on every reboot.
     */
    private fun onInstallSucceeded() {
        // Read the pause before clearing it: a loop the user paused on purpose
        // must not be silently re-armed by a manual install.
        val wasSuspended = AutoRunManager.isSuspended(app)
        AutoRunManager.recordSuccess(app)
        val stable = mutableState.value.systemStable
        if (detectInstalled() && stable && !wasSuspended) {
            AppPreferences.setAutoRunOnBoot(app, true)
        }
    }

    /**
     * A failed boot attempt either schedules the next try with a doubled wait
     * or pauses auto-run for good, and both outcomes are logged so the pause is
     * visible in the installer log and in settings.
     */
    private fun onAutoRunAttemptFailed() {
        val decision = AutoRunManager.recordFailure(app, interrupted = false)
        when (decision.action) {
            AutoRunAction.Retry -> {
                appendLog(app.getString(R.string.auto_run_retry_scheduled, decision.retryInSeconds))
                AutoRunScheduler.schedule(app, decision.retryInSeconds)
            }

            AutoRunAction.Suspend -> appendLog(app.getString(R.string.auto_run_suspended))
            AutoRunAction.Proceed -> Unit
        }
    }

    private suspend fun executeExploit(payload: File) {
        val shizuku = shizukuEnabled()
        val logFile = if (shizuku) File(SHIZUKU_LOG_PATH) else File(app.filesDir, "exploit.log")
        if (shizuku) {
            ShizukuController.exec(arrayOf("rm", "-f", SHIZUKU_LOG_PATH)).waitFor()
        } else {
            logFile.delete()
        }
        val helper = helperFile()
        if (!shizuku) {
            require(helper.canExecute()) { app.getString(R.string.error_helper_unavailable) }
        }
        val logPrefix = mutableState.value.log
        val bootToken = currentBootToken()
        val process = if (shizuku) {
            val stagedPayload = shizukuStage(payload, SHIZUKU_PAYLOAD_PATH, "755")
            ShizukuController.exec(
                arrayOf("/system/bin/sh", "-c", "true"),
                shizukuEnvironment(bootToken, stagedPayload.absolutePath, helper.absolutePath),
            )
        } else {
            val processBuilder = ProcessBuilder(
                helper.absolutePath,
                "--run-payload",
                payload.absolutePath,
                helper.absolutePath,
                logFile.absolutePath,
            ).redirectErrorStream(true)
            processBuilder.environment().apply {
                put("EXPLOIT_ATTEMPTS", EXPLOIT_ATTEMPTS)
                put("P0_ATTEMPT_TIMEOUT_SEC", P0_ATTEMPT_TIMEOUT_SEC)
                put("EXPLOIT_ATTEMPT_TIMEOUT_SEC", EXPLOIT_ATTEMPT_TIMEOUT_SEC)
                cachedP0Offset(bootToken)?.let { put(P0_OFFSET_ENV, it) }
            }
            processBuilder.start()
        }
        val captured = StringBuilder()
        val readLog: () -> String = if (shizuku) {
            { drainProcessOutput(process, captured) }
        } else {
            // Keep draining stdout while polling: if the helper fills the OS
            // pipe buffer it blocks on write and stops making log progress,
            // which would trip the stall detector spuriously.
            { drainProcessOutput(process, captured); logFile.readTextIfPresent() }
        }

        try {
            val startedAt = SystemClock.elapsedRealtime()
            var lastProgressAt = startedAt
            var lastRawLog = ""
            while (process.isAlive) {
                val rawLog = readLog()
                if (rawLog != lastRawLog) {
                    cacheP0Offset(bootToken, rawLog)
                    publishExploitLog(logPrefix, rawLog)
                    lastRawLog = rawLog
                    lastProgressAt = SystemClock.elapsedRealtime()
                }
                val now = SystemClock.elapsedRealtime()
                require(now - lastProgressAt < EXPLOIT_STALL_MILLIS) {
                    app.getString(R.string.error_exploit_stalled)
                }
                require(now - startedAt < EXPLOIT_TOTAL_MILLIS) {
                    app.getString(R.string.error_exploit_timeout)
                }
                delay(if (shizuku) SHIZUKU_LOG_POLL_INTERVAL else LOG_POLL_INTERVAL)
            }

            val exitCode = process.waitFor()
            val rawLog = readLog()
            cacheP0Offset(bootToken, rawLog)
            publishExploitLog(logPrefix, rawLog)
            // Both transports drain into `captured` during the poll loop, so
            // this never blocks on a child still holding the pipe open.
            val earlyOutput = captured.toString().trim()

            // SIGKILL (137) means the process was externally killed — the kernel
            // may already have been partially modified. Treat this differently
            // from a clean non-zero exit so the user gets an accurate diagnosis.
            if (exitCode == 137) {
                throw IllegalArgumentException(
                    app.getString(R.string.error_payload_killed, earlyOutput.takeIf(String::isNotBlank)
                        ?.let { " ($it)" } ?: ""),
                )
            }
            require(exitCode == 0) {
                app.getString(
                    R.string.error_payload_exit,
                    exitCode,
                    earlyOutput.takeIf(String::isNotBlank)?.let { " ($it)" } ?: "",
                )
            }
            require(rawLog.contains("exploit completed") && rawLog.contains("done=1 root=1")) {
                app.getString(R.string.error_success_marker)
            }
        } finally {
            // process.waitFor() has already returned above, so the process
            // is guaranteed dead. Only call destroy() if somehow still alive
            // (e.g., the wait loop above was interrupted before reaching waitFor).
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                }
            }
        }
        appendLog(app.getString(R.string.log_bootstrap_root))
    }

    private fun drainProcessOutput(process: Process, buffer: StringBuilder): String {
        return try {
            drainStream(process.inputStream, buffer)
            drainStream(process.errorStream, buffer)
            buffer.toString()
        } catch (_: Throwable) {
            buffer.toString()
        }
    }

    private fun drainStream(stream: InputStream, buffer: StringBuilder) {
        val data = ByteArray(4096)
        while (stream.available() > 0) {
            val count = stream.read(data, 0, data.size)
            if (count <= 0) break
            buffer.append(String(data, 0, count, Charsets.UTF_8))
        }
    }

    private fun publishExploitLog(prefix: String, rawLog: String) {
        mutableState.value = mutableState.value.copy(
            log = listOf(prefix, stripAnsi(rawLog))
                .filter(String::isNotBlank)
                .joinToString("\n"),
        )
        updateHistoryLog()
    }

    private suspend fun installKernelSu(payloads: VerifiedPayloads) {
        if (shizukuEnabled()) {
            shizukuStage(payloads.kernelSu, SHIZUKU_KSUD_PATH, "755")
            shizukuStage(payloads.kernelSu, SHIZUKU_KSUD_STAGE_PATH, "755")
            appendLog(app.getString(R.string.log_ksu_staged))
        } else {
            val source = shellQuote(payloads.kernelSu.absolutePath)
            val stageCommand =
                "/system/bin/cp $source $SHIZUKU_KSUD_PATH && " +
                    "/system/bin/cp $source $SHIZUKU_KSUD_STAGE_PATH && " +
                    "/system/bin/chmod 755 $SHIZUKU_KSUD_PATH $SHIZUKU_KSUD_STAGE_PATH"
            val stage = runHelper("-c", stageCommand)
            require(stage.code == 0) { app.getString(R.string.error_ksu_stage, stage.output) }
            appendLog(app.getString(R.string.log_ksu_staged))
        }

        val lateLoad = runHelper("--late-load")
        require(lateLoad.code == 0) {
            app.getString(R.string.error_ksu_verify, lateLoad.code, lateLoad.output)
        }
        if (lateLoad.output.isNotBlank()) appendLog(lateLoad.output)
        storeInstallReceipt()
        appendLog(app.getString(R.string.log_ksu_control_verified))
        verifyStability()
    }

    /**
     * Verifies that the system remains stable after the KernelSU payload was applied.
     * A payload can apply root correctly but still break SystemUI overlays and
     * Google apps (e.g., by modifying display or SELinux properties). If the system
     * is not stable, auto-run on boot is NOT enabled, and the user must manually
     * verify the system works before re-enabling auto-run.
     */
    private fun verifyStability() {
        val systemUiAlive = runCatching { isSystemUiAlive() }.getOrNull() ?: true
        val stability = if (systemUiAlive) InstallStability.Stable else InstallStability.Unstable

        mutableState.value = mutableState.value.copy(stability = stability)

        if (stability == InstallStability.Unstable) {
            appendLog("[-] System stability check failed: SystemUI not responsive")
            appendLog("[!] Auto-run on boot is NOT enabled. Manually verify the system works before retrying.")
        } else {
            appendLog("[+] System stability check passed")
        }
    }

    private fun isSystemUiAlive(): Boolean {
        return try {
            val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val processCheck = activityManager?.runningAppProcesses?.any {
                it.processName.contains("systemui", ignoreCase = true) &&
                    it.importance >= android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
            } ?: false
            if (processCheck) return true
            // Fallback: check if SystemUI package is present and not stopped
            val pm = app.packageManager
            val state = pm.getApplicationInfo("com.android.systemui", 0)
            state != null && !pm.getComponentEnabledSetting(
                ComponentName("com.android.systemui", "com.android.systemui.SystemUI")
            ).let { it == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || it == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER }
        } catch (_: Throwable) {
            true
        }
    }

    private fun isExploitRunning(): Boolean {
        // An auto-run attempt is armed but this boot has no verified receipt,
        // so the payload is mid-flight. The attempt marker is cleared when the
        // run finishes, so a stale flag can no longer pin the UI to "running".
        if (AutoRunManager.isAttemptInProgress(app) && !hasVerifiedReceipt()) return true
        // Check if the exploit helper process is still running
        return runCatching {
            File("/proc").listFiles()?.any { dir ->
                runCatching {
                    val cmd = File(dir, "cmdline").readText(Charsets.UTF_8).trim()
                    cmd.contains("libcve43499root") || cmd.contains("ksud-s25u-kdp") || cmd.contains("cve-2026-43499")
                }.getOrNull() ?: false
            } ?: false
        }.getOrNull() ?: false
    }

    private fun hasVerifiedReceipt(): Boolean = runCatching {
        val bootToken = currentBootToken() ?: return false
        val receipt = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
        receipt.getString(RECEIPT_BOOT_TOKEN, null) == bootToken && receipt.getBoolean(RECEIPT_VERIFIED, false)
    }.getOrNull() ?: false

    private fun detectInstalled(): Boolean {
        if (NativeProbe.isAvailable() && runCatching { NativeProbe.isKernelSuActive() }.getOrNull() == true) return true
        val bootToken = currentBootToken() ?: return false
        val receipt = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
        return receipt.getString(RECEIPT_BOOT_TOKEN, null) == bootToken &&
            receipt.getBoolean(RECEIPT_VERIFIED, false)
    }

    private fun storeInstallReceipt() {
        val bootToken = currentBootToken() ?: error(app.getString(R.string.error_boot_id))
        val stored = app.getSharedPreferences(INSTALL_RECEIPT, Application.MODE_PRIVATE)
            .edit()
            .putString(RECEIPT_BOOT_TOKEN, bootToken)
            .putBoolean(RECEIPT_VERIFIED, true)
            .commit()
        require(stored) { app.getString(R.string.error_receipt) }
    }

    private fun currentBootToken(): String? = runCatching {
        File("/proc/sys/kernel/random/boot_id")
            .readText(Charsets.US_ASCII)
            .trim()
            .takeIf(String::isNotBlank)
    }.getOrNull()

    private fun cachedP0Offset(bootToken: String?): String? {
        if (bootToken == null) return null
        val stored = app.getSharedPreferences(P0_CACHE, Application.MODE_PRIVATE)
        if (stored.getString(P0_CACHE_BOOT_TOKEN, null) != bootToken) return null
        return stored.getString(P0_CACHE_OFFSET, null)
    }

    private fun cacheP0Offset(bootToken: String?, log: String) {
        if (bootToken == null) return
        val match = P0_OFFSET_PATTERN.findAll(log).lastOrNull() ?: return
        val offset = match.groupValues[1].toLongOrNull(16) ?: return
        if (offset !in 0..P0_OFFSET_MAX || offset and P0_OFFSET_MASK != 0L) return
        val value = "0x${offset.toString(16)}"
        val stored = app.getSharedPreferences(P0_CACHE, Application.MODE_PRIVATE)
        if (stored.getString(P0_CACHE_BOOT_TOKEN, null) == bootToken &&
            stored.getString(P0_CACHE_OFFSET, null) == value
        ) return
        stored.edit()
            .putString(P0_CACHE_BOOT_TOKEN, bootToken)
            .putString(P0_CACHE_OFFSET, value)
            .apply()
    }

    private fun helperFile(): File =
        if (shizukuEnabled()) {
            shizukuStage(nativeHelperFile(), SHIZUKU_HELPER_PATH, "755")
        } else {
            nativeHelperFile()
        }

    private fun nativeHelperFile() = File(app.applicationInfo.nativeLibraryDir, "libcve43499root.so")

    private fun shizukuEnabled(): Boolean = activeRunShizuku ?: AppPreferences.shizukuMode(app)

    private fun shizukuStage(source: File, target: String, mode: String): File {
        val staged = File(target)
        if (stagedFileIsCurrent(staged, source)) return staged
        try {
            ShizukuController.writeFile(target, mode, source.inputStream())
        } catch (error: Throwable) {
            throw IllegalStateException(
                app.getString(R.string.error_shizuku_stage, target, error.message.orEmpty()),
                error,
            )
        }
        return staged
    }

    private fun shizukuEnvironment(
        bootToken: String?,
        payloadPath: String,
        helperPath: String,
    ): Array<String> = buildList {
        add("EXPLOIT_ATTEMPTS=$EXPLOIT_ATTEMPTS")
        add("P0_ATTEMPT_TIMEOUT_SEC=$P0_ATTEMPT_TIMEOUT_SEC")
        add("EXPLOIT_ATTEMPT_TIMEOUT_SEC=$EXPLOIT_ATTEMPT_TIMEOUT_SEC")
        add("CVE43499_ROOT_HELPER=$helperPath")
        add("LD_PRELOAD=$payloadPath")
        cachedP0Offset(bootToken)?.let { add("$P0_OFFSET_ENV=$it") }
    }.toTypedArray()

    /**
     * Runs the bootstrap helper for a short management command. Unlike the
     * exploit run there is no log file to poll, so output is drained inline
     * and a hard deadline guards against a helper that never exits — without
     * this, a hung `--late-load` leaves the install stuck in LoadingKernelSu
     * indefinitely.
     */
    private suspend fun runHelper(vararg arguments: String): CommandResult {
        val helper = helperFile()
        val process = if (shizukuEnabled()) {
            ShizukuController.exec(arrayOf(helper.absolutePath) + arguments)
        } else {
            ProcessBuilder(listOf(helper.absolutePath) + arguments)
                .redirectErrorStream(true)
                .start()
        }
        val captured = StringBuilder()
        val startedAt = SystemClock.elapsedRealtime()
        try {
            while (process.isAlive) {
                drainProcessOutput(process, captured)
                require(SystemClock.elapsedRealtime() - startedAt < HELPER_TIMEOUT_MILLIS) {
                    app.getString(
                        R.string.error_helper_timeout,
                        captured.toString().trim().takeIf(String::isNotBlank)
                            ?.let { ": $it" } ?: "",
                    )
                }
                delay(HELPER_POLL_INTERVAL)
            }
            drainProcessOutput(process, captured)
            val exitCode = process.waitFor()
            return CommandResult(exitCode, stripAnsi(captured.toString().trim()))
        } finally {
            // process.waitFor() has already returned, so the process
            // is guaranteed dead. Only destroy if somehow still alive.
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                }
            }
        }
    }

    private fun shellQuote(value: String) = "'${value.replace("'", "'\\''")}'"

    private fun setPhase(phase: InstallPhase, message: String) {
        mutableState.value = mutableState.value.copy(phase = phase, message = message)
        appendLog("[*] $message")
    }

    private fun appendLog(line: String) {
        val cleanLine = stripAnsi(line).trim()
        if (cleanLine.isBlank()) return
        mutableState.value = mutableState.value.copy(
            log = (mutableState.value.log + "\n" + cleanLine).trim(),
        )
        updateHistoryLog()
    }

    private fun startHistory() {
        val entry = historyStore.create()
        activeHistoryEntry = entry
        publishHistory(entry)
    }

    private fun updateHistory(transform: (InstallHistoryEntry) -> InstallHistoryEntry) {
        val entry = activeHistoryEntry ?: return
        val updated = transform(entry)
        activeHistoryEntry = updated
        historyStore.save(updated)
        publishHistory(updated)
    }

    private fun updateHistoryLog() =
        updateHistory { it.copy(log = mutableState.value.log) }

    private fun updateHistoryProfile(profileId: String) =
        updateHistory { it.copy(profileId = profileId) }

    private fun finishHistory(result: InstallRunResult) {
        updateHistory { entry ->
            entry.copy(
                completedAtMillis = System.currentTimeMillis(),
                result = result,
                log = mutableState.value.log,
            )
        }
        activeHistoryEntry = null
    }

    private fun publishHistory(entry: InstallHistoryEntry) {
        mutableHistory.value = (mutableHistory.value.filterNot { it.id == entry.id } + entry)
            .sortedByDescending(InstallHistoryEntry::startedAtMillis)
    }

    private fun File.readTextIfPresent(): String = if (exists()) readText() else ""

    companion object {
        private const val EXPLOIT_ATTEMPTS = "24"
        private const val P0_ATTEMPT_TIMEOUT_SEC = "45"
        private const val EXPLOIT_ATTEMPT_TIMEOUT_SEC = "120"
        private const val EXPLOIT_STALL_MILLIS = 90_000L
        private const val EXPLOIT_TOTAL_MILLIS = 900_000L
        private const val HELPER_TIMEOUT_MILLIS = 120_000L
        private const val INSTALL_RECEIPT = "install_receipt"
        private const val RECEIPT_BOOT_TOKEN = "kernel_boot_id"
        private const val RECEIPT_VERIFIED = "verified"
        private const val P0_CACHE = "p0_cache"
        private const val P0_CACHE_BOOT_TOKEN = "kernel_boot_id"
        private const val P0_CACHE_OFFSET = "offset"
        private const val P0_OFFSET_ENV = "SLIDE_P0_OFFSET"
        private const val P0_OFFSET_MAX = 0x1f0000L
        private const val P0_OFFSET_MASK = 0xffffL
        private const val SHIZUKU_LOG_PATH = "/data/local/tmp/ksu-exploit.log"
        private const val SHIZUKU_HELPER_PATH = "/data/local/tmp/ksu-helper"
        private const val SHIZUKU_PAYLOAD_PATH = "/data/local/tmp/ksu-payload"
        private const val SHIZUKU_KSUD_PATH = "/data/local/tmp/ksud-s25u-kdp"
        private const val SHIZUKU_KSUD_STAGE_PATH = "/data/local/tmp/.ksud-stage"
        private val LOG_POLL_INTERVAL = 250.milliseconds
        private val HELPER_POLL_INTERVAL = 250.milliseconds
        private val SHIZUKU_LOG_POLL_INTERVAL = 1.seconds
        private val ANSI_ESCAPE = Regex("\u001B\\[[0-?]*[ -/]*[@-~]")
        private val P0_OFFSET_PATTERN = Regex(
            "slide-kaslr-ok[^\\n]*slide=([0-9a-fA-F]{16})",
        )

        private fun stripAnsi(value: String): String = ANSI_ESCAPE.replace(value, "").replace("\r", "")
    }
}
