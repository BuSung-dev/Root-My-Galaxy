package dev.busung.s25uroot

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val apkUrl: String?,
    val releaseUrl: String,
)

const val ROOT_MY_GALAXY_URL = "https://github.com/BuSung-dev/Root-My-Galaxy"

object AppUpdater {

    private const val GITHUB_API = "https://api.github.com/repos/BuSung-dev/Root-My-Galaxy"
    private const val RELEASES_PAGE = "$ROOT_MY_GALAXY_URL/releases/latest"

    suspend fun fetchLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL("$GITHUB_API/releases/latest").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "RootMyGalaxy/${BuildConfig.VERSION_NAME}")
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }

                val json = JSONObject(body)
                val tag = json.optString("tag_name").trim().removePrefix("v")
                if (tag.isBlank()) return@withContext null
                var apkUrl: String? = null
                json.optJSONArray("assets")?.let { assets ->
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.optString("name").endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url").ifEmpty { null }
                            break
                        }
                    }
                }
                UpdateInfo(
                    versionName = tag,
                    apkUrl = apkUrl,
                    releaseUrl = json.optString("html_url").ifEmpty { RELEASES_PAGE },
                )
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isUpdateAvailable(latestVersion: String, currentVersion: String): Boolean =
        latestVersion.isNotEmpty() && latestVersion != currentVersion

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "update.apk")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "RootMyGalaxy/${BuildConfig.VERSION_NAME}")
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val total = connection.contentLength
                val buffer = ByteArray(64 * 1024)
                var downloaded = 0L
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                if (target.length() == 0L) return@withContext null
                target
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            target.delete()
            null
        }
    }

    fun installApk(context: Context, apk: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun openReleasesPage(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE)))
    }
}
