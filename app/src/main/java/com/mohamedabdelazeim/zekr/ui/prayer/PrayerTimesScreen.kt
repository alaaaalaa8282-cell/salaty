package com.mohamedabdelazeim.zekr.ui.prayer

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.ui.theme.DarkGreen
import com.mohamedabdelazeim.zekr.ui.theme.Gold
import com.mohamedabdelazeim.zekr.ui.theme.GoldGradient
import com.mohamedabdelazeim.zekr.ui.theme.LightGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PrayerTimesScreen(
    viewModel: PrayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val prayerState by viewModel.prayerState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()

    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            viewModel.loadPrayerTimes()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrayerHeader(
                location = locationState.locationName ?: "جاري تحديد الموقع...",
                hijriDate = prayerState.hijriDate ?: "",
                gregorianDate = prayerState.gregorianDate ?: "",
                isLoading = prayerState.isLoading,
                onRefresh = {
                    if (locationPermissionState.status.isGranted) {
                        viewModel.refreshPrayerTimes()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!locationPermissionState.status.isGranted) {
                PermissionCard(
                    onRequestPermission = { locationPermissionState.launchPermissionRequest() }
                )
            } else {
                when {
                    prayerState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Gold)
                        }
                    }
                    prayerState.error != null -> {
                        ErrorCard(
                            message = prayerState.error ?: "حدث خطأ",
                            onRetry = { viewModel.refreshPrayerTimes() }
                        )
                    }
                    else -> {
                        prayerState.nextPrayer?.let { nextPrayer ->
                            NextPrayerCard(nextPrayer = nextPrayer)
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        PrayerTimesList(
                            prayers = prayerState.prayers,
                            nextPrayerName = prayerState.nextPrayer?.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerHeader(
    location: String,
    hijriDate: String,
    gregorianDate: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkGreen.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = location,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = if (isLoading) Color.Gray else Gold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = hijriDate,
                color = Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = gregorianDate,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun NextPrayerCard(nextPrayer: PrayerItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(GoldGradient),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الصلاة القادمة",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nextPrayer.name,
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nextPrayer.time,
                    color = Color.Black,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = nextPrayer.remaining,
                    color = Color.Black.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PrayerTimesList(
    prayers: List<PrayerItem>,
    nextPrayerName: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkGreen.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            prayers.forEach { prayer ->
                PrayerTimeRow(
                    prayer = prayer,
                    isNext = prayer.name == nextPrayerName
                )
                if (prayer != prayers.last()) {
                    Divider(
                        color = Color.White.copy(alpha = 0.2f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrayerTimeRow(
    prayer: PrayerItem,
    isNext: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isNext) Gold.copy(alpha = 0.15f) else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNext) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = prayer.name,
                color = if (isNext) Gold else Color.White,
                fontSize = 18.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            text = prayer.time,
            color = if (isNext) Gold else Color.White.copy(alpha = 0.9f),
            fontSize = 20.sp,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkGreen.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "الوصول إلى الموقع مطلوب",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لحساب مواقيت الصلاة بدقة حسب موقعك الجغرافي",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("منح الصلاحية", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = DarkGreen.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إعادة المحاولة", fontSize = 14.sp)
            }
        }
    }
}

data class PrayerItem(
    val name: String,
    val time: String,
    val remaining: String = ""
)

data class PrayerState(
    val prayers: List<PrayerItem> = emptyList(),
    val nextPrayer: PrayerItem? = null,
    val hijriDate: String? = null,
    val gregorianDate: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class LocationState(
    val locationName: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
