package com.ahmoodiy.prayer.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.ahmoodiy.prayer.domain.model.CustomAudioMeta
import java.io.File

/**
 * Owns the user-imported adhan file.
 *
 * The picked document is copied into app-private storage, so playback stays fully offline and
 * does not depend on a persisted SAF permission.
 */
class CustomAdhanStore(context: Context) {

    private val appContext = context.applicationContext

    private val directory: File
        get() = File(appContext.filesDir, "adhan").apply { mkdirs() }

    fun customFile(): File? = File(directory, FILE_NAME).takeIf { it.exists() }

    fun exists(): Boolean = customFile() != null

    /**
     * Copies the document behind [uri] into private storage and returns its metadata.
     * Returns null when the content cannot be read (or exceeds the size cap).
     */
    fun import(uri: Uri, displayName: String, mimeType: String?): CustomAudioMeta? {
        return runCatching {
            val resolver = appContext.contentResolver
            val target = File(directory, FILE_NAME)

            val copied = resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            if (target.length() <= 0L || target.length() > MAX_BYTES) {
                target.delete()
                return null
            }

            CustomAudioMeta(
                name = displayName.ifBlank { "custom_adhan" },
                size = target.length(),
                type = mimeType ?: "audio/*",
                durationMs = readDuration(target),
                addedAtEpochMs = System.currentTimeMillis(),
            )
        }.getOrNull()
    }

    fun clear() {
        runCatching { customFile()?.delete() }
    }

    private fun readDuration(file: File): Long? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } finally {
            retriever.release()
        }
    }.getOrNull()

    companion object {
        /** Single-slot storage: importing again simply replaces the previous file. */
        private const val FILE_NAME = "custom_adhan.mp3"

        /** 40 MB ceiling: far above any real adhan clip, but protects private storage. */
        private const val MAX_BYTES = 40L * 1024 * 1024
    }
}
