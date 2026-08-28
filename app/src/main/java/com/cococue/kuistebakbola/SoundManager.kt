package com.cococue.kuistebakbola

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundManager {
    private var soundPool: SoundPool? = null
    private var soundMap = mutableMapOf<String, Int>()
    private var isLoaded = false

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }

        // Memuat suara secara dinamis agar tidak error jika file belum ada di res/raw/
        loadSound(context, "correct")
        loadSound(context, "wrong")
        loadSound(context, "victory")
    }

    private fun loadSound(context: Context, name: String) {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId != 0) {
            soundMap[name] = soundPool?.load(context, resId, 1) ?: 0
        }
    }

    fun playSound(context: Context, soundName: String) {
        val soundId = soundMap[soundName]
        if (isLoaded && soundId != null && soundId != 0) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun vibrate(context: Context, duration: Long = 200) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}