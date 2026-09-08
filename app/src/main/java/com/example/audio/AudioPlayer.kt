package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.os.Build
import android.media.PlaybackParams

enum class RepeatMode(val count: Int) {
    OFF(0),
    ONCE(1),
    THREE_TIMES(3),
    LOOP(999)
}

sealed class PlayerState {
    object Idle : PlayerState()
    object Loading : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    data class Error(val message: String) : PlayerState()
}

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentSource = MutableStateFlow<String?>(null)
    val currentSource: StateFlow<String?> = _currentSource.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var currentRemainingRepeats = 0

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { mp ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mp.isPlaying) {
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        currentRemainingRepeats = mode.count
    }

    fun play(dataSource: String, onComplete: (() -> Unit)? = null) {
        if (_currentSource.value == dataSource && mediaPlayer != null) {
            if (_playerState.value is PlayerState.Paused) {
                mediaPlayer?.start()
                _playerState.value = PlayerState.Playing
                startProgressTracker()
                return
            }
        }

        stop()

        _currentSource.value = dataSource
        _playerState.value = PlayerState.Loading
        currentRemainingRepeats = _repeatMode.value.count

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (dataSource.startsWith("http://") || dataSource.startsWith("https://")) {
                    setDataSource(context, Uri.parse(dataSource))
                } else {
                    setDataSource(dataSource)
                }

                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _playbackSpeed.value != 1.0f) {
                        try {
                            mp.playbackParams = mp.playbackParams.setSpeed(_playbackSpeed.value)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    mp.start()
                    _playerState.value = PlayerState.Playing
                    startProgressTracker()
                }

                setOnCompletionListener { mp ->
                    if (currentRemainingRepeats > 0) {
                        if (currentRemainingRepeats != RepeatMode.LOOP.count) {
                            currentRemainingRepeats--
                        }
                        mp.seekTo(0)
                        mp.start()
                    } else {
                        _playerState.value = PlayerState.Idle
                        _positionMs.value = _durationMs.value
                        stopProgressTracker()
                        onComplete?.invoke()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    _playerState.value = PlayerState.Error("Erreur d'écoute audio ($what, $extra)")
                    stopProgressTracker()
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player

        } catch (e: Exception) {
            e.printStackTrace()
            _playerState.value = PlayerState.Error("Impossible de lire la source audio: ${e.localizedMessage}")
        }
    }

    fun pause() {
        if (_playerState.value is PlayerState.Playing) {
            mediaPlayer?.pause()
            _playerState.value = PlayerState.Paused
            stopProgressTracker()
        }
    }

    fun resume() {
        if (_playerState.value is PlayerState.Paused) {
            mediaPlayer?.start()
            _playerState.value = PlayerState.Playing
            startProgressTracker()
        }
    }

    fun togglePlayPause(dataSource: String, onComplete: (() -> Unit)? = null) {
        if (_currentSource.value == dataSource) {
            when (_playerState.value) {
                is PlayerState.Playing -> pause()
                is PlayerState.Paused -> resume()
                else -> play(dataSource, onComplete)
            }
        } else {
            play(dataSource, onComplete)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying || _playerState.value is PlayerState.Paused) {
                mp.seekTo(positionMs.toInt())
                _positionMs.value = positionMs
            }
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
        _playerState.value = PlayerState.Idle
        _currentSource.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (_playerState.value is PlayerState.Playing) {
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            _positionMs.value = mp.currentPosition.toLong()
                        }
                    } catch (e: Exception) {
                        // ignore transient errors during release
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
