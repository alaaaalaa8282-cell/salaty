package com.mohamedabdelazeim.zekr.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.MainActivity
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.data.ZekrData
import com.mohamedabdelazeim.zekr.data.ZekrPrefs

class ZekrService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val CHANNEL_ID = "zekr_channel"
        const val NOTIF_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // تحقق من المكالمات والأذان
        if (isCallActive() || isAudioBusy()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val index = ZekrPrefs.nextZekrIndex(this)
        val zekr = ZekrData.zekrList[index]

        val notif = buildNotification(zekr.name, zekr.text)
        startForeground(NOTIF_ID, notif)

        if (zekr.audioRes != null) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, zekr.audioRes)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                stopSelf()
            }
            mediaPlayer?.start()
        } else {
            android.os.Handler(mainLooper).postDelayed({ stopSelf() }, 5000)
        }

        return START_NOT_STICKY
    }

    private fun isCallActive(): Boolean {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (e: Exception) { false }
    }

    private fun isAudioBusy(): Boolean {
        return try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isMusicActive
        } catch (e: Exception) { false }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val bmp = BitmapFactory.decodeResource(resources, R.drawable.notification_father)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(bmp)
            .setContentTitle("🤲 $title")
            .setContentText(text.take(80))
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bmp)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
                    .setSummaryText(text.take(100))
            )
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "أذكار", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "إشعارات الأذكار الصوتية"
            enableVibration(true)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
