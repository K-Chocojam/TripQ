
package com.tripq.tripq

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class MusicMelodyPlayer {

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    private val sampleRate = 44100

    // 기존 오리지널 테스트 멜로디 10개 유지
    private val melodies = listOf(
        listOf(523.25, 659.25, 783.99, 659.25, 523.25),
        listOf(392.00, 440.00, 523.25, 440.00, 392.00),
        listOf(659.25, 783.99, 880.00, 783.99, 659.25),
        listOf(440.00, 523.25, 659.25, 783.99, 659.25),
        listOf(587.33, 523.25, 440.00, 523.25, 587.33),
        listOf(392.00, 523.25, 659.25, 523.25, 783.99),
        listOf(783.99, 659.25, 587.33, 523.25, 440.00),
        listOf(523.25, 783.99, 659.25, 880.00, 783.99),
        listOf(440.00, 587.33, 659.25, 587.33, 523.25),
        listOf(659.25, 587.33, 523.25, 440.00, 392.00)
    )

    // 기존 테스트 멜로디 재생
    @Synchronized
    fun play(questionIndex: Int) {
        stop()

        val melody = melodies[questionIndex % melodies.size]
        val samples = generateMelody(melody)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack = track

            track.write(samples, 0, samples.size)
            track.play()

        } catch (e: Exception) {
            stop()
        }
    }

    // 휴대폰에서 선택한 음악 파일 재생
    @Synchronized
    fun play(context: Context, uri: Uri) {
        stop()

        try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            player.setDataSource(context.applicationContext, uri)

            player.setOnPreparedListener { preparedPlayer ->
                if (mediaPlayer === preparedPlayer) {
                    preparedPlayer.start()
                } else {
                    preparedPlayer.release()
                }
            }

            player.setOnCompletionListener { completedPlayer ->
                if (mediaPlayer === completedPlayer) {
                    mediaPlayer = null
                    completedPlayer.release()
                }
            }

            player.setOnErrorListener { errorPlayer, _, _ ->
                if (mediaPlayer === errorPlayer) {
                    mediaPlayer = null
                    errorPlayer.release()
                }
                true
            }

            player.prepareAsync()

        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun generateMelody(notes: List<Double>): ShortArray {
        val noteDuration = 350
        val gapDuration = 60

        val result = ArrayList<Short>()

        notes.forEach { frequency ->
            val noteSamples = sampleRate * noteDuration / 1000
            val gapSamples = sampleRate * gapDuration / 1000

            for (i in 0 until noteSamples) {
                val time = i.toDouble() / sampleRate

                val fadeIn = (i / 500.0).coerceIn(0.0, 1.0)
                val fadeOut =
                    ((noteSamples - i) / 700.0).coerceIn(0.0, 1.0)

                val envelope = min(fadeIn, fadeOut)

                val wave = sin(
                    2.0 * PI * frequency * time
                )

                val sample = (
                        wave * envelope * 10000
                        ).toInt().toShort()

                result.add(sample)
            }

            repeat(gapSamples) {
                result.add(0)
            }
        }

        return result.toShortArray()
    }

    // 모든 재생 중지
    @Synchronized
    fun stop() {
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (_: Exception) {
        }

        audioTrack = null

        try {
            mediaPlayer?.let { player ->
                player.setOnPreparedListener(null)
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                player.release()
            }
        } catch (_: Exception) {
        }

        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
