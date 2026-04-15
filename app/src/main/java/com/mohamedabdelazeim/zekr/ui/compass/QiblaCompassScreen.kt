package com.mohamedabdelazeim.zekr.ui.compass

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.*
import com.mohamedabdelazeim.zekr.ui.theme.DarkGreen
import com.mohamedabdelazeim.zekr.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QiblaCompassScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )

    var currentAzimuth by remember { mutableStateOf(0f) }
    var qiblaDirection by remember { mutableStateOf(0f) }
    var distanceToKaaba by remember { mutableStateOf(0.0) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var accuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationVectorSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    // موقع الكعبة
    val kaabaLocation = remember {
        Location("Kaaba").apply {
            latitude = 21.422487
            longitude = 39.826206
        }
    }

    // مرشح الترددات المنخفضة (Low-Pass Filter)
    val alpha = 0.05f
    var filteredAzimuth by remember { mutableStateOf(0f) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rotationMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)

                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)

                        val azimuthInRadians = orientation[0]
                        var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

                        // تحويل إلى درجات موجبة (0-360)
                        azimuthInDegrees = (azimuthInDegrees + 360) % 360

                        // تطبيق مرشح الترددات المنخفضة
                        filteredAzimuth = filteredAzimuth * (1 - alpha) + azimuthInDegrees * alpha
                        currentAzimuth = filteredAzimuth

                        accuracy = it.accuracy
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // دالة حساب اتجاه القبلة
    fun calculateQiblaDirection(lat: Double, lng: Double): Float {
        val lat1 = Math.toRadians(lat)
        val lng1 = Math.toRadians(lng)
        val lat2 = Math.toRadians(kaabaLocation.latitude)
        val lng2 = Math.toRadians(kaabaLocation.longitude)

        val dLng = lng2 - lng1
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        var qibla = Math.toDegrees(atan2(y, x)).toFloat()
        qibla = (qibla + 360) % 360

        return qibla
    }

    // دالة حساب المسافة إلى الكعبة
    fun calculateDistance(lat: Double, lng: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            lat, lng,
            kaabaLocation.latitude, kaabaLocation.longitude,
            results
        )
        return results[0].toDouble()
    }

    // الحصول على الموقع الحالي
    fun getCurrentLocation() {
        isLoading = true
        error = null

        if (!locationPermissionState.status.isGranted) {
            isLoading = false
            error = "صلاحية الموقع غير ممنوحة"
            return
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                isLoading = false
                error = "الرجاء تفعيل خدمة تحديد الموقع"
                return
            }

            @Suppress("MissingPermission")
            val lastKnownLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

            if (lastKnownLocation != null) {
                currentLocation = lastKnownLocation
                qiblaDirection = calculateQiblaDirection(lastKnownLocation.latitude, lastKnownLocation.longitude)
                distanceToKaaba = calculateDistance(lastKnownLocation.latitude, lastKnownLocation.longitude)
                isLoading = false
            } else {
                isLoading = false
                error = "تعذر الحصول على الموقع"
            }
        } catch (e: Exception) {
            isLoading = false
            error = e.message
        }
    }

    // تسجيل وإلغاء تسجيل المستشعر
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    rotationVectorSensor?.let {
                        sensorManager.registerListener(
                            sensorEventListener,
                            it,
                            SensorManager.SENSOR_DELAY_GAME
                        )
                    }
                    getCurrentLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(sensorEventListener)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(sensorEventListener)
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
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = DarkGreen.copy(alpha = 0.85f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اتجاه القبلة",
                        color = Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { getCurrentLocation() },
                        enabled = !isLoading && locationPermissionState.status.isGranted
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = if (isLoading) Color.Gray else Gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                !locationPermissionState.status.isGranted -> {
                    PermissionCard(onRequestPermission = { locationPermissionState.launchPermissionRequest() })
                }
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold)
                    }
                }
                error != null -> {
                    ErrorCard(message = error!!, onRetry = { getCurrentLocation() })
                }
                else -> {
                    // البوصلة
                    CompassView(
                        azimuth = currentAzimuth,
                        qiblaDirection = qiblaDirection,
                        accuracy = accuracy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // معلومات إضافية
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
                                .padding(20.dp)
                        ) {
                            // اتجاه القبلة بالدرجات
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "درجة القبلة:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${qiblaDirection.toInt()}°",
                                    color = Gold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // المسافة إلى الكعبة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المسافة إلى الكعبة:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${distanceToKaaba.toInt()} كم",
                                    color = Gold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // دقة البوصلة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "دقة البوصلة:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = when (accuracy) {
                                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "عالية"
                                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "متوسطة"
                                        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "منخفضة"
                                        else -> "غير معروفة"
                                    },
                                    color = when (accuracy) {
                                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF4CAF50)
                                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompassView(
    azimuth: Float,
    qiblaDirection: Float,
    accuracy: Int,
    modifier: Modifier = Modifier
) {
    val animatedQiblaRotation by animateFloatAsState(
        targetValue = qiblaDirection - azimuth,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "qiblaRotation"
    )

    val animatedCompassRotation by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "compassRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) - 20.dp.toPx()

            // رسم دائرة البوصلة
            rotate(animatedCompassRotation, Offset(centerX, centerY)) {
                // الدائرة الخارجية
                drawCircle(
                    color = DarkGreen,
                    radius = radius,
                    style = Stroke(width = 8.dp.toPx())
                )

                // علامات الدرجات
                for (i in 0..35) {
                    val angle = i * 10f
                    val isMain = i % 3 == 0
                    val startRadius = if (isMain) radius - 30.dp.toPx() else radius - 20.dp.toPx()
                    val endRadius = radius - 5.dp.toPx()

                    val rad = Math.toRadians(angle.toDouble())
                    val cos = cos(rad).toFloat()
                    val sin = sin(rad).toFloat()

                    val startX = centerX + cos * startRadius
                    val startY = centerY + sin * startRadius
                    val endX = centerX + cos * endRadius
                    val endY = centerY + sin * endRadius

                    drawLine(
                        color = if (isMain) Color.White else Color.White.copy(alpha = 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMain) 3.dp.toPx() else 1.5.dp.toPx()
                    )

                    // كتابة الجهات
                    if (i % 9 == 0) {
                        val textRadius = radius - 50.dp.toPx()
                        val textX = centerX + cos * textRadius
                        val textY = centerY + sin * textRadius

                        val direction = when (i) {
                            0 -> "ش"
                            9 -> "ش"
                            18 -> "ج"
                            27 -> "غ"
                            else -> ""
                        }

                        if (direction.isNotEmpty()) {
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 24.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                                drawText(
                                    direction,
                                    textX,
                                    textY - (paint.descent() + paint.ascent()) / 2,
                                    paint
                                )
                            }
                        }
                    }
                }

                // سهم الشمال
                drawPath(
                    path = Path().apply {
                        val arrowSize = 30.dp.toPx()
                        moveTo(centerX, centerY - radius + 10.dp.toPx())
                        lineTo(centerX - arrowSize / 2, centerY - radius + arrowSize + 10.dp.toPx())
                        lineTo(centerX + arrowSize / 2, centerY - radius + arrowSize + 10.dp.toPx())
                        close()
                    },
                    color = Gold
                )
            }

            // اتجاه القبلة
            rotate(animatedQiblaRotation, Offset(centerX, centerY)) {
                drawLine(
                    color = Gold,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, centerY - radius + 20.dp.toPx()),
                    strokeWidth = 6.dp.toPx()
                )

                // مثلث القبلة
                drawPath(
                    path = Path().apply {
                        val arrowSize = 25.dp.toPx()
                        moveTo(centerX, centerY - radius + 15.dp.toPx())
                        lineTo(centerX - arrowSize, centerY - radius + arrowSize + 15.dp.toPx())
                        lineTo(centerX + arrowSize, centerY - radius + arrowSize + 15.dp.toPx())
                        close()
                    },
                    color = Gold
                )
            }

            // الدائرة المركزية
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 15.dp.toPx(),
                center = Offset(centerX, centerY)
            )

            // كتابة الكعبة
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FFD700")
                    textSize = 16.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                drawText(
                    "🕋",
                    centerX,
                    centerY - (paint.descent() + paint.ascent()) / 2,
                    paint
                )
            }
        }

        // مؤشر الدقة
        if (accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حرك هاتفك بشكل 8 لتحسين الدقة",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
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
                text = "لتحديد اتجاه القبلة بدقة حسب موقعك الجغرافي",
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
