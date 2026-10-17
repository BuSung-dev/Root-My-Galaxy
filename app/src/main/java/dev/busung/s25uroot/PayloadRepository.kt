package dev.busung.s25uroot

import android.content.Context
import android.os.Environment
import android.system.Os
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class VerifiedPayloads(
    val profile: TargetProfile,
    val exploit: File,
    val kernelSu: File,
)

class PayloadRepository(private val context: Context) {
    private val repository: String
        get() = AppPreferences.payloadRepository(context)
    private val branch: String
        get() = AppPreferences.payloadBranch(context)

    private val internalPayloadDir by lazy {
        File(context.filesDir, "payloads")
    }

    private val externalPayloadDir by lazy {
        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        File(downloads, "RootMyGalaxy/payloads")
    }

    fun loadTargets(): List<TargetProfile> {
        val commit = resolveMainCommit()
        val manifestBytes = downloadBytes(rawUrl(commit, "support/targets-v3.json"), MAX_MANIFEST_BYTES)
        return SupportManifest.parse(manifestBytes).targets.map { profile -> profile.copy(
            exploit = profile.exploit.copy(url = pinArtifactUrl(profile.exploit.url, commit)),
            kernelSu = profile.kernelSu.copy(url = pinArtifactUrl(profile.kernelSu.url, commit)),
        ) }
    }

    fun resolveTarget(snapshot: DeviceSnapshot): TargetProfile = loadTargets()
        .firstOrNull { it.matches(snapshot) }
        ?: error(context.getString(R.string.repo_no_profile))

    fun resolveTarget(profileId: String): TargetProfile = loadTargets()
        .firstOrNull { it.profileId == profileId }
        ?: error(context.getString(R.string.repo_profile_missing, profileId))

    /**
     * Download payloads and save to both internal and external storage.
     * Returns VerifiedPayloads with the internal storage files.
     */
    fun download(profile: TargetProfile, onProgress: (String) -> Unit): VerifiedPayloads {
        val internalDir = File(internalPayloadDir, profile.profileId).apply { mkdirs() }
        val externalDir = File(externalPayloadDir, profile.profileId).apply { mkdirs() }

        val exploit = downloadArtifactToBoth(
            profile.exploit,
            File(internalDir, "cve-2026-43499-app.so"),
            File(externalDir, "cve-2026-43499-app.so"),
            context.getString(R.string.artifact_exploit),
            onProgress,
        )
        val kernelSu = downloadArtifactToBoth(
            profile.kernelSu,
            File(internalDir, "ksud-s25u-kdp"),
            File(externalDir, "ksud-s25u-kdp"),
            context.getString(R.string.artifact_kernelsu),
            onProgress,
        )
        Os.chmod(exploit.absolutePath, 0b100100100)
        Os.chmod(kernelSu.absolutePath, 0b100100100)
        return VerifiedPayloads(profile, exploit, kernelSu)
    }

    /**
     * Check if payloads already exist (in internal or external storage)
     * for the given profile ID without downloading.
     */
    fun payloadsExistLocally(profileId: String): Boolean {
        val internalProfileDir = File(internalPayloadDir, profileId)
        val externalProfileDir = File(externalPayloadDir, profileId)
        val hasInternal = File(internalProfileDir, "cve-2026-43499-app.so").exists() &&
            File(internalProfileDir, "ksud-s25u-kdp").exists()
        val hasExternal = File(externalProfileDir, "cve-2026-43499-app.so").exists() &&
            File(externalProfileDir, "ksud-s25u-kdp").exists()
        return hasInternal || hasExternal
    }

    /**
     * Get the locally stored payload files for a profile.
     * Returns null if not found in either storage location.
     */
    fun getLocalPayloads(profileId: String): VerifiedPayloads? {
        val internalDir = File(internalPayloadDir, profileId)
        val externalDir = File(externalPayloadDir, profileId)

        val internalExploit = File(internalDir, "cve-2026-43499-app.so")
        val internalKernelSu = File(internalDir, "ksud-s25u-kdp")
        if (internalExploit.exists() && internalKernelSu.exists()) {
            val profile = resolveTarget(profileId)
            return VerifiedPayloads(profile, internalExploit, internalKernelSu)
        }

        val externalExploit = File(externalDir, "cve-2026-43499-app.so")
        val externalKernelSu = File(externalDir, "ksud-s25u-kdp")
        if (externalExploit.exists() && externalKernelSu.exists()) {
            val profile = resolveTarget(profileId)
            return VerifiedPayloads(profile, externalExploit, externalKernelSu)
        }

        return null
    }

    /**
     * Copy payloads from internal storage to external storage for persistence.
     * Returns true if successful.
     */
    fun copyPayloadsToExternal(profileId: String): Boolean {
        val internalDir = File(internalPayloadDir, profileId)
        val externalDir = File(externalPayloadDir, profileId).apply { mkdirs() }

        val internalExploit = File(internalDir, "cve-2026-43499-app.so")
        val internalKernelSu = File(internalDir, "ksud-s25u-kdp")

        if (!internalExploit.exists() || !internalKernelSu.exists()) return false

        return try {
            copyFile(internalExploit, File(externalDir, "cve-2026-43499-app.so"))
            copyFile(internalKernelSu, File(externalDir, "ksud-s25u-kdp"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete payloads from external storage.
     */
    fun deleteExternalPayloads(profileId: String) {
        val externalDir = File(externalPayloadDir, profileId)
        externalDir.deleteRecursively()
    }

    /**
     * Get the external storage directory path for display purposes.
     */
    fun getExternalStoragePath(): String {
        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, "RootMyGalaxy/payloads").absolutePath
    }

    private fun downloadArtifactToBoth(
        artifact: RemoteArtifact,
        internalDest: File,
        externalDest: File,
        label: String,
        onProgress: (String) -> Unit,
    ): File {
        onProgress(context.getString(R.string.repo_downloading, label))

        val temporary = File(internalDest.parentFile, "${internalDest.name}.part")
        val downloadedFile = downloadArtifactToFile(artifact, temporary, label, onProgress)

        internalDest.parentFile?.mkdirs()
        if (internalDest.exists()) internalDest.delete()
        require(downloadedFile.renameTo(internalDest)) {
            context.getString(R.string.repo_finalize_failed, label)
        }

        try {
            externalDest.parentFile?.mkdirs()
            if (externalDest.exists()) externalDest.delete()
            copyFile(internalDest, externalDest)
        } catch (e: Exception) {
            // Non-critical: external storage copy failure doesn't block installation
        }

        Os.chmod(internalDest.absolutePath, 0b100100100)
        if (externalDest.exists()) {
            Os.chmod(externalDest.absolutePath, 0b100100100)
        }
        onProgress(context.getString(R.string.repo_verified, label))
        return internalDest
    }

    private fun downloadArtifactToFile(
        artifact: RemoteArtifact,
        destination: File,
        label: String,
        onProgress: (String) -> Unit,
    ): File {
        onProgress(context.getString(R.string.repo_downloading, label))
        val connection = open(artifact.url)
        require(connection.contentLengthLong == -1L || connection.contentLengthLong == artifact.size) {
            context.getString(R.string.repo_size_mismatch, label)
        }
        var total = 0L
        val temporary = File(destination.parentFile, "${destination.name}.part")
        connection.inputStream.use { input ->
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= artifact.size) {
                        context.getString(R.string.repo_size_exceeded, label)
                    }
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        connection.disconnect()
        require(total == artifact.size) { context.getString(R.string.repo_incomplete, label) }
        if (destination.exists()) destination.delete()
        require(temporary.renameTo(destination)) {
            context.getString(R.string.repo_finalize_failed, label)
        }
        onProgress(context.getString(R.string.repo_verified, label))
        return destination
    }

    private fun copyFile(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        source.inputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output, DEFAULT_BUFFER_SIZE)
            }
        }
        destination.setExecutable(true, false)
    }

    private fun resolveMainCommit(): String {
        val response = downloadBytes(commitApiUrl(), MAX_COMMIT_RESPONSE_BYTES)
        val commit = JSONObject(response.toString(Charsets.UTF_8))
            .getJSONObject("object")
            .getString("sha")
        require(commit.matches(Regex("[0-9a-f]{40}"))) { context.getString(R.string.repo_commit_invalid) }
        return commit
    }

    private fun commitApiUrl() =
        "https://api.github.com/repos/$repository/git/refs/heads/$branch"

    private fun rawRepository() =
        "https://raw.githubusercontent.com/$repository"

    private fun rawUrl(commit: String, path: String) = "${rawRepository()}/$commit/$path"

    private fun mutableRawPrefix() = "${rawRepository()}/$branch/"

    private fun legacyMutableRawPrefix() =
        "https://raw.githubusercontent.com/${AppPreferences.DEFAULT_PAYLOAD_REPOSITORY}/" +
            "${AppPreferences.DEFAULT_PAYLOAD_BRANCH}/"

    private fun pinArtifactUrl(url: String, commit: String): String {
        val prefix = mutableRawPrefix()
        val legacyPrefix = legacyMutableRawPrefix()
        val relative = when {
            url.startsWith(prefix) -> url.removePrefix(prefix)
            url.startsWith(legacyPrefix) -> url.removePrefix(legacyPrefix)
            else -> error(context.getString(R.string.repo_url_invalid))
        }
        return "${rawRepository()}/$commit/$relative"
    }

    private fun downloadBytes(url: String, maximum: Int): ByteArray {
        val connection = open(url)
        val bytes = connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= maximum) {
                    context.getString(R.string.repo_response_too_large)
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        connection.disconnect()
        return bytes
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "S25URoot/${BuildConfig.VERSION_NAME}")
            connect()
            require(responseCode == HttpURLConnection.HTTP_OK) { "HTTP $responseCode" }
        }

    companion object {
        private const val MAX_COMMIT_RESPONSE_BYTES = 16 * 1024
        private const val MAX_MANIFEST_BYTES = 256 * 1024
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
