package com.ahmoodiy.prayer.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Small vibration helper for the rosary.
 * Everything degrades silently on devices without a vibrator.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    val isAvailable: Boolean get() = vibrator?.hasVibrator() == true

    /** A crisp tap for every bead. */
    fun tap() = vibrate(VibrationEffect.createOneShot(16L, 60))

    /** Slightly stronger feedback when a cycle/target completes. */
    fun cycleComplete() = vibrate(VibrationEffect.createOneShot(38L, 110))

    /** Two-pulse pattern used for finished targets. */
    fun doubleTap() = vibrate(
        VibrationEffect.createWaveform(longArrayOf(0L, 26L, 70L, 26L), intArrayOf(0, 120, 0, 120), -1),
    )

    private fun vibrate(effect: VibrationEffect) {
        val target = vibrator ?: return
        if (!target.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                target.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                target.vibrate(30L)
            }
        }
    }

    fun release() {
        runCatching { vibrator?.cancel() }
    }
}
