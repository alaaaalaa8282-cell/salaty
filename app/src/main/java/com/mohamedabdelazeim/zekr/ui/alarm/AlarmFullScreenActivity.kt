package com.mohamedabdelazeim.zekr.ui.alarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.mohamedabdelazeim.zekr.service.audio.AudioFocusManager
import com.mohamedabdelazeim.zekr.ui.theme.DarkGreen
import com.mohamedabdelazeim.zekr.ui.theme.Gold
import com.mohamedabdelazeim.zekr.ui.theme.GoldGradient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmFullScreenActivity : ComponentActivity() {

    @Inject
    lateinit var audioFocusManager: AudioFocusManager

    private var prayerName: String = ""
    private var prayerTime: String = ""
    private var audioUri: Uri? = null
    private var audioResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // جعل الشاشة تعمل حتى لو الجهاز مقفول
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // منع الشاشة من القفل
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // قراءة البيانات من الـ Intent
        prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        prayerTime = intent.getStringExtra("prayer_time") ?: ""
        audioResId = intent.getIntExtra("audio_res_id", 0)
        val audioUriString = intent.getStringExtra("audio_uri")
        if (!audioUriString.isNullOrEmpty()) {
            audioUri = Uri.parse(audioUriString)
        }

        setContent {
            AlarmFullScreenContent(
                prayerName = prayerName,
                prayerTime = prayerTime,
                onStop = { finish() },
                onSnooze = {
                    // تأجيل 5 دقائق
                    finish()
                }
            )
        }

        // تشغيل صوت الأذان
        playAdhan()
    }

    private fun playAdhan() {
        if (audioResId != 0) {
            audioFocusManager.requestAudioFocusAndPlay(audioResId) {
                // عند انتهاء الأذان
            }
        } else if (audioUri != null) {
            audioFocusManager.requestAudioFocusAndPlayUri(audioUri!!) {
                // عند انتهاء الأذان
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioFocusManager.stopPlayback()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun AlarmFullScreenContent(
    prayerName: String,
    prayerTime: String,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkGreen,
                        Color.Black
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // أيقونة الصلاة
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(60.dp))
                    .background(
                        Brush.radialGradient(
                            colors = GoldGradient,
                            radius = 120f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // اسم الصلاة
            Text(
                text = prayerName,
                color = Gold,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // وقت الصلاة
            Text(
                text = prayerTime,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "حان الآن"
            Text(
                text = "حان الآن",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // أزرار التحكم
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // زر التأجيل
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأجيل 5 دقائق", fontSize = 16.sp)
                }

                // زر الإيقاف
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إيقاف", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
