package com.mohamedabdelazeim.zekr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SalaatiApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // قناة إشعارات الصلاة (أهمية عالية للصوت والاهتزاز)
            val prayerChannel = NotificationChannel(
                "prayer_channel",
                "أوقات الصلاة والأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول وقت الصلاة وتشغيل الأذان"
                setSound(null, null)
                enableVibration(true)
            }

            // قناة إشعارات الأذكار (أهمية منخفضة - تعمل بصمت)
            val azkarChannel = NotificationChannel(
                "azkar_channel",
                "الأذكار المتكررة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تشغيل الأذكار في الخلفية"
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(prayerChannel)
            notificationManager.createNotificationChannel(azkarChannel)
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
