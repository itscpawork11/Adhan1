package com.ahmoodiy.prayer.audio

import android.content.Context
import com.ahmoodiy.prayer.domain.model.AdhanVoice
import java.io.File
import kotlin.math.pow

/**
 * Renders a melodic, adhan-inspired call on-device for every selectable voice.
 *
 * The result is written once to the cache directory as a WAV file so it can be streamed by
 * [AdhanPlayer] with proper duration/stop handling. Nothing is downloaded.
 */
object AdhanSynth {

    private const val CACHE_VERSION = 1

    /** Semitone offsets forming the maqam-like scale used by the melodies. */
    private val scale = intArrayOf(0, 2, 3, 5, 7, 8, 10, 12, 14)

    private data class VoiceSpec(
        val rootSemitones: Double,
        val contour: List<Int>,
        val noteMs: Int,
        val phraseGapMs: Int,
        val repeats: Int,
        val amplitude: Double,
    )

    /**
     * Melody spec per voice. Only [AdhanVoice.TAKBEERAT] and [AdhanVoice.CUSTOM] are normally
     * synthesised; the rest ship as bundled recordings, and this is their safety fallback.
     */
    private fun specFor(voice: AdhanVoice): VoiceSpec = when (voice) {
        AdhanVoice.TAKBEERAT -> VoiceSpec(-5.0, listOf(4, 4, 4, 2, 2, 0), 340, 260, 2, 0.66)
        AdhanVoice.CUSTOM -> VoiceSpec(-4.0, listOf(6, 5, 3, 2, 1, 0), 560, 460, 2, 0.60)
        AdhanVoice.MAKKAH -> VoiceSpec(0.0, listOf(8, 7, 6, 4, 3, 2, 0, -1), 520, 420, 3, 0.58)
        else -> VoiceSpec(-7.0, listOf(8, 7, 5, 4, 3, 2, 1, 0), 620, 520, 3, 0.62)
    }

    /** Stable cache path for a rendered voice. */
    fun cacheFile(context: Context, voice: AdhanVoice): File =
        File(context.cacheDir, "adhan_synth_v${CACHE_VERSION}_${voice.id}.wav")

    /** Generates (once) and returns the WAV file for the given voice. Blocking: call from IO. */
    fun ensureRendered(context: Context, voice: AdhanVoice): File {
        val target = cacheFile(context, voice)
        if (target.exists() && target.length() > 1024) return target
        val samples = render(voice)
        PcmSynth.writeWav(target, samples)
        return target
    }

    /** Deletes every rendered clip (used when clearing app data / regenerating). */
    fun clearCache(context: Context) {
        AdhanVoice.ordered.forEach { voice ->
            runCatching { cacheFile(context, voice).delete() }
        }
    }

    fun render(voice: AdhanVoice): ShortArray {
        val spec = specFor(voice)
        val notes = ArrayList<PcmSynth.Note>()
        var cursorMs = 700

        repeat(spec.repeats) { pass ->
            spec.contour.forEachIndexed { index, degree ->
                // Small ornamentation: every other pass lifts the last note of the phrase.
                val adjusted = if (pass == 1 && index == spec.contour.lastIndex) degree + 2 else degree
                val frequency = hz(spec.rootSemitones + scale[adjusted.coerceIn(0, scale.lastIndex)])
                notes += PcmSynth.Note(
                    frequencyHz = frequency,
                    startMs = cursorMs,
                    durationMs = spec.noteMs,
                    amplitude = spec.amplitude,
                )
                cursorMs += (spec.noteMs * 0.84).toInt()
            }
            cursorMs += spec.phraseGapMs
        }

        val totalMs = cursorMs + 1400
        return PcmSynth.renderMelody(notes, totalMs)
    }

    private fun hz(semitonesFromA4: Double): Double =
        440.0 * 2.0.pow(semitonesFromA4 / 12.0)
}
