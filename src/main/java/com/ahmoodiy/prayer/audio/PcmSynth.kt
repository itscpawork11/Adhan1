package com.ahmoodiy.prayer.audio

import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tiny offline tone generator.
 *
 * The app ships no audio assets at all: every sound is synthesised on-device, which keeps the
 * APK small and the experience 100% offline. Output is 16-bit mono PCM at [SAMPLE_RATE].
 */
object PcmSynth {

    const val SAMPLE_RATE = 44_100

    /** Wooden bead tap: a short, low-passed noise burst with a fast exponential decay. */
    fun woodClick(durationMs: Int = 70): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        val out = ShortArray(count)
        val random = Random(0x5EED)
        var filtered = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 55.0)
            val noise = random.nextDouble() * 2.0 - 1.0
            filtered = filtered * 0.65 + noise * 0.35
            val value = filtered * envelope * 0.80
            out[i] = toPcm(value)
        }
        return out
    }

    /** Crystal bead tap: a bright decaying bell with one harmonic. */
    fun crystalPing(durationMs: Int = 420): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 8.0)
            val fundamental = sin(2.0 * PI * 1760.0 * t)
            val harmonic = 0.45 * sin(2.0 * PI * 2640.0 * t)
            val value = (fundamental + harmonic) * envelope * 0.45
            out[i] = toPcm(value)
        }
        return out
    }

    /** A single note event inside a synthesised melody. */
    data class Note(
        val frequencyHz: Double,
        val startMs: Int,
        val durationMs: Int,
        val amplitude: Double = 0.5,
    )

    /**
     * Renders a melody into a single PCM buffer.
     * Notes may overlap; the renderer mixes them additively and soft-clips the result.
     */
    fun renderMelody(notes: List<Note>, totalMs: Int, vibratoHz: Double = 5.0, vibratoDepth: Double = 0.006): ShortArray {
        val total = SAMPLE_RATE * totalMs / 1000
        val mix = DoubleArray(total)
        notes.forEach { note ->
            val start = SAMPLE_RATE * note.startMs / 1000
            val length = SAMPLE_RATE * note.durationMs / 1000
            val attack = (SAMPLE_RATE * 0.035).toInt().coerceAtLeast(1)
            for (i in 0 until length) {
                val index = start + i
                if (index >= total) break
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / length
                val envelope = when {
                    i < attack -> i.toDouble() / attack
                    else -> exp(-(progress - 0.05) * 2.6)
                }.coerceIn(0.0, 1.0)
                val wobble = 1.0 + vibratoDepth * sin(2.0 * PI * vibratoHz * t)
                val phase = 2.0 * PI * note.frequencyHz * wobble * t
                // Fundamental plus a gentle second harmonic for a warmer "voice".
                val sample = sin(phase) + 0.28 * sin(2.0 * phase)
                mix[index] += sample * envelope * note.amplitude
            }
        }
        val out = ShortArray(total)
        for (i in 0 until total) out[i] = toPcm(mix[i] * 0.55)
        return out
    }

    /** Writes PCM samples as a 16-bit mono WAV file so MediaPlayer can stream them. */
    fun writeWav(file: File, samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { stream ->
            val dataSize = samples.size * 2
            stream.write("RIFF".toByteArray())
            stream.write(intLe(36 + dataSize))
            stream.write("WAVE".toByteArray())
            stream.write("fmt ".toByteArray())
            stream.write(intLe(16))
            stream.write(shortLe(1)) // PCM
            stream.write(shortLe(1)) // mono
            stream.write(intLe(sampleRate))
            stream.write(intLe(sampleRate * 2)) // byte rate
            stream.write(shortLe(2)) // block align
            stream.write(shortLe(16)) // bits per sample
            stream.write("data".toByteArray())
            stream.write(intLe(dataSize))
            val bytes = ByteArray(dataSize)
            for (i in samples.indices) {
                val s = samples[i].toInt()
                bytes[i * 2] = (s and 0xFF).toByte()
                bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
            }
            stream.write(bytes)
        }
    }

    private fun toPcm(value: Double): Short =
        (value.coerceIn(-1.0, 1.0) * 32_000.0).toInt().toShort()

    private fun intLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

    private fun shortLe(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
    )
}
