package com.amh.sotto.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ChimePlayer {
    private const val SAMPLE_RATE = 44100
    private var cachedBuffer: ShortArray? = null

    init {
        cachedBuffer = generateChimeBuffer()
    }

    /**
     * Generates a pleasant two-tone musical chime:
     * Note 1: D5 (587.33 Hz) for 90ms with gentle decay
     * Note 2: A5 (880.00 Hz) for 160ms with smooth bell-like exponential decay
     */
    private fun generateChimeBuffer(): ShortArray {
        val duration1 = 0.09
        val duration2 = 0.16
        val totalDuration = duration1 + duration2
        val totalSamples = (totalDuration * SAMPLE_RATE).toInt()
        val numSamples1 = (duration1 * SAMPLE_RATE).toInt()
        val numSamples2 = totalSamples - numSamples1

        val buffer = ShortArray(totalSamples)
        val freq1 = 587.33
        val freq2 = 880.00

        // Note 1
        for (i in 0 until numSamples1) {
            val t = i.toDouble() / SAMPLE_RATE
            val angle = 2.0 * PI * freq1 * t
            val attack = (i.toDouble() / (0.01 * SAMPLE_RATE)).coerceAtMost(1.0)
            val decay = 1.0 - (i.toDouble() / numSamples1) * 0.3
            val envelope = attack * decay
            val sample = (sin(angle) * envelope * Short.MAX_VALUE * 0.75).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Note 2
        for (i in 0 until numSamples2) {
            val t = i.toDouble() / SAMPLE_RATE
            val angle = 2.0 * PI * freq2 * t
            val attack = (i.toDouble() / (0.008 * SAMPLE_RATE)).coerceAtMost(1.0)
            val decay = exp(-4.5 * (i.toDouble() / numSamples2))
            val envelope = attack * decay
            val sample = ((sin(angle) * 0.8 + sin(2.0 * angle) * 0.2) * envelope * Short.MAX_VALUE * 0.75).toInt()
            buffer[numSamples1 + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buffer
    }

    fun play(context: Context) {
        // Haptic feedback
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (e: Exception) {
            // Safe fallback if device lacks vibrator
        }

        // Audio playback on STREAM_MUSIC (unaffected by ringer / silent / vibrate mode)
        thread(start = true, isDaemon = true) {
            try {
                val buffer = cachedBuffer ?: generateChimeBuffer()

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                Thread.sleep(260)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore audio playback errors
            }
        }
    }
}
