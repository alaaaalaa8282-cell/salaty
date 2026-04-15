package com.mohamedabdelazeim.zekr.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFocusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _audioState = MutableStateFlow(AudioState.IDLE)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                _audioState.value = AudioState.PLAYING
                mediaPlayer?.start()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                _audioState.value = AudioState.STOPPED
                stopPlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _audioState.value = AudioState.PAUSED
                mediaPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
        }
    }

    fun isInCall(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telecomManager?.isInCall == true
            } else {
                audioManager.mode == AudioManager.MODE_IN_CALL
            }
        } catch (e: Exception) {
            false
        }
    }

    fun requestAudioFocusAndPlay(
        audioResId: Int,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        if (isInCall()) {
            return false
        }

        val focusGranted = requestAudioFocus()
        if (!focusGranted) {
            return false
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$audioResId"))
                prepare()
                setOnCompletionListener {
                    _audioState.value = AudioState.COMPLETED
                    onComplete?.invoke()
                    abandonAudioFocus()
                }
                start()
            }
            _audioState.value = AudioState.PLAYING
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            abandonAudioFocus()
            return false
        }
    }

    fun requestAudioFocusAndPlayUri(
        audioUri: android.net.Uri,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        if (isInCall()) {
            return false
        }

        val focusGranted = requestAudioFocus()
        if (!focusGranted) {
            return false
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, audioUri)
                prepare()
                setOnCompletionListener {
                    _audioState.value = AudioState.COMPLETED
                    onComplete?.invoke()
                    abandonAudioFocus()
                }
                start()
            }
            _audioState.value = AudioState.PLAYING
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            abandonAudioFocus()
            return false
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        abandonAudioFocus()
        _audioState.value = AudioState.STOPPED
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.pause()
            _audioState.value = AudioState.PAUSED
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resumePlayback(): Boolean {
        if (isInCall()) {
            return false
        }

        return try {
            mediaPlayer?.start()
            _audioState.value = AudioState.PLAYING
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusListener)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    }

    fun release() {
        stopPlayback()
    }
}

enum class AudioState {
    IDLE,
    PLAYING,
    PAUSED,
    STOPPED,
    COMPLETED
}
