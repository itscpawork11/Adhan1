package com.ahmoodiy.prayer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.ahmoodiy.prayer.R
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import java.io.File

/**
 * Plays the adhan. Source priority for each voice:
 *  1. the user's own file (imported through SAF) when [AdhanVoice.CUSTOM] is selected,
 *  2. a real bundled recording shipped in `res/raw` (the famous muezzins),
 *  3. a synthesised fallback rendered on device (e.g. [AdhanVoice.TAKBEERAT]).
 *
 * A single instance is reused so a new call always replaces the previous one.
 */
class AdhanPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    /** Set to true for a test preview; the alarm flow uses the alarm stream. */
    val isPlaying: Boolean get() = player?.isPlaying == true

    /**
     * Starts playback and returns true when audio actually began.
     * [onComplete] is invoked when the clip finishes or fails.
     */
    fun play(
        voice: AdhanVoice,
        customFile: File?,
        useAlarmStream: Boolean = false,
        onComplete: (() -> Unit)? = null,
    ): Boolean {
        stop()

        val customSource = if (voice == AdhanVoice.CUSTOM) customFile?.takeIf { it.exists() } else null
        val rawRes = if (customSource == null) rawResourceFor(voice) else null
        val synthSource = if (customSource == null && rawRes == null) {
            runCatching { AdhanSynth.ensureRendered(appContext, voice) }.getOrNull()
        } else {
            null
        }

        if (customSource == null && rawRes == null && synthSource == null) {
            onComplete?.invoke()
            return false
        }

        return runCatching {
            val attributes = AudioAttributes.Builder()
                .setUsage(
                    if (useAlarmStream) AudioAttributes.USAGE_ALARM
                    else AudioAttributes.USAGE_MEDIA,
                )
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                isLooping = false
                setOnCompletionListener {
                    releaseQuietly()
                    onComplete?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    releaseQuietly()
                    onComplete?.invoke()
                    true
                }
            }

            when {
                customSource != null -> mediaPlayer.setDataSource(customSource.absolutePath)
                rawRes != null -> {
                    val afd = runCatching { appContext.resources.openRawResourceFd(rawRes) }.getOrNull()
                        ?: throw IllegalStateException("raw adhan resource unavailable: $rawRes")
                    try {
                        mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    } finally {
                        runCatching { afd.close() }
                    }
                }
                else -> mediaPlayer.setDataSource(synthSource!!.absolutePath)
            }

            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            true
        }.getOrElse {
            releaseQuietly()
            onComplete?.invoke()
            false
        }
    }

    fun stop() {
        runCatching {
            player?.let { media ->
                if (media.isPlaying) media.stop()
            }
        }
        releaseQuietly()
    }

    private fun releaseQuietly() {
        runCatching { player?.release() }
        player = null
    }

    /** The bundled recording for [voice], or null when the voice is synthesised. */
    private fun rawResourceFor(voice: AdhanVoice): Int? = when (voice) {
        AdhanVoice.MAKKAH -> R.raw.adhan_makkah
        AdhanVoice.MADINAH -> R.raw.adhan_madinah
        AdhanVoice.AQSA -> R.raw.adhan_alaqsa
        AdhanVoice.EGYPT -> R.raw.adhan_egypt
        AdhanVoice.ABDULBASIT -> R.raw.abdul_basit
        AdhanVoice.MINSHAWI -> R.raw.minshawi
        AdhanVoice.HOSSARY -> R.raw.hossary
        AdhanVoice.ALAFASY -> R.raw.mishary_alafasy
        AdhanVoice.QATAMI -> R.raw.nasser_alqatami
        AdhanVoice.YUSUF_ISLAM -> R.raw.yusuf_islam
        AdhanVoice.TAKBEERAT, AdhanVoice.CUSTOM -> null
    }
}
