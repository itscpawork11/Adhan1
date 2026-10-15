package com.ahmoodiy.prayer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.ahmoodiy.prayer.domain.model.TasbeehSoundType
import java.util.concurrent.Executors

/**
 * Plays the short, synthesised bead taps of the digital rosary.
 *
 * Buffers are generated once and cached; playback happens on a small background pool so rapid
 * tapping never blocks the UI thread and overlapping taps mix naturally.
 */
class BeadSoundPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val pool = Executors.newFixedThreadPool(3)

    private val woodSamples by lazy { PcmSynth.woodClick() }
    private val crystalSamples by lazy { PcmSynth.crystalPing() }

    /** Volume of the taps relative to the notification/audio stream. */
    var volume: Float = 0.85f

    fun play(type: TasbeehSoundType) {
        val samples = when (type) {
            TasbeehSoundType.WOOD -> woodSamples
            TasbeehSoundType.CRYSTAL -> crystalSamples
            TasbeehSoundType.MUTE -> return
        }
        pool.execute { playBlocking(samples) }
    }

    /** Long-press feedback: a slightly deeper, double tap. */
    fun playCycleComplete(type: TasbeehSoundType) {
        if (type == TasbeehSoundType.MUTE) return
        play(type)
        pool.execute {
            Thread.sleep(90)
            playBlocking(if (type == TasbeehSoundType.CRYSTAL) crystalSamples else woodSamples)
        }
    }

    private fun playBlocking(samples: ShortArray) {
        var track: AudioTrack? = null
        try {
            val sizeBytes = samples.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(PcmSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(sizeBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.setVolume(volume)
            track.write(samples, 0, samples.size)
            track.play()

            // Keep the track alive for the duration of the buffer, then release it.
            val durationMs = (samples.size * 1000L) / PcmSynth.SAMPLE_RATE
            Thread.sleep(durationMs + 60L)
            track.stop()
        } catch (_: Throwable) {
            // Audio is a nicety: never let a playback failure crash the rosary.
        } finally {
            runCatching { track?.release() }
        }
    }

    /** True when the device is muted on the notification stream. */
    fun isDeviceMuted(): Boolean {
        val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return audio.ringerMode == AudioManager.RINGER_MODE_SILENT
    }

    fun release() {
        pool.shutdownNow()
    }
}
