package dev.busung.s25uroot

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object CustomPayloadStore {
    private const val PREFERENCES = "appearance"
    private const val DISPLAY_NAME = "custom_exploit_name"
    private const val MAX_BYTES = 16 * 1024 * 1024
    private const val FILE_NAME = "custom-exploit.so"

    fun file(context: Context): File? = File(context.filesDir, FILE_NAME)
        .takeIf { it.isFile && it.length() > 0L }

    fun displayName(context: Context): String? = file(context)?.let {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(DISPLAY_NAME, null)
            ?: it.name
    }

    fun import(context: Context, uri: Uri): String {
        val name = queryDisplayName(context, uri) ?: FILE_NAME
        require(name.endsWith(".so", ignoreCase = true)) {
            context.getString(R.string.custom_payload_not_so)
        }

        val destination = File(context.filesDir, FILE_NAME)
        val temporary = File(context.filesDir, "$FILE_NAME.part")
        var total = 0L
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { context.getString(R.string.custom_payload_read_failed) }
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BYTES) { context.getString(R.string.custom_payload_too_large) }
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        }
        require(total >= 4L && temporary.inputStream().use { input ->
            val magic = ByteArray(4)
            input.read(magic) == 4 && magic.contentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46))
        }) { context.getString(R.string.custom_payload_not_elf) }

        if (destination.exists()) destination.delete()
        require(temporary.renameTo(destination)) { context.getString(R.string.repo_finalize_failed, name) }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(DISPLAY_NAME, name)
            .apply()
        return name
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
        File(context.filesDir, "$FILE_NAME.part").delete()
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(DISPLAY_NAME)
            .apply()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
}
