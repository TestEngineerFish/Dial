package com.example.dial

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import java.text.DecimalFormat

internal fun Float.formatTimeString(): String {
    val decimalFormat = DecimalFormat("00")
    return decimalFormat.format(this) + ":00"
}

internal fun Context.vibrator() {
    val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java)
    if (vibrator?.hasVibrator() == true) {
        val pattern = longArrayOf(0, 10)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }
}
