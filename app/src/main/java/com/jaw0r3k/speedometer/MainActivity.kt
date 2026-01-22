package com.jaw0r3k.speedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    var currentSpeed = 0.0F;
    private lateinit var serviceIntent: Intent;

    private val sateliteCount: MutableLiveData<Int> = MutableLiveData<Int>(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        serviceIntent = Intent(this, MyNavigationService::class.java)
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions();
        } else {
            Log.i("Has permissions", "YAY");
            startLocationUpdates()
        }


        enableEdgeToEdge()
        setContent {
            val currentSpeedState by LocalizationDataManager.speedLiveData.asFlow()
                .collectAsState(initial = 0f)
            val maxSpeedState by LocalizationDataManager.maxSpeedLiveData.asFlow()
                .collectAsState(initial = 0f)
            val averageSpeedState by LocalizationDataManager.averageSpeedLiveData.asFlow()
                .collectAsState(initial = 0f)

            val distanceTravelledState by LocalizationDataManager.distanceTravelledLiveData.asFlow()
                .collectAsState(initial = 0f)

            val accuracyState by LocalizationDataManager.accuracyLiveData.asFlow()
                .collectAsState(initial = 0f)

            val sateliteCountState by sateliteCount.asFlow()
                .collectAsState(initial = 0)

            NeoAnalogSpeedometerScreen(
                currentSpeedKmh = currentSpeedState,
                maxSpeedKmh = maxSpeedState,
                avgSpeedKmh = averageSpeedState,
                distanceKm = distanceTravelledState.toFloat(),
                isTripActive = true,
                sateliteCount = sateliteCountState,
                onResetClick = {
                    LocalizationDataManager.reset();
//                    isTripActive = !isTripActive
//                    // Simulate speed change for preview
//                    if(isTripActive) currentSpeed = 75f else currentSpeed = 0f
                },
                onSettingsClick = {},
                accuracy = accuracyState,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        awaitClose {
//            locationManager.unregisterGnssStatusCallback(callback)
//        }
//        stopService(serviceIntent)
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val callback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                // Offer the latest status to the flow
                sateliteCount.value = status.satelliteCount
            }
        }

        locationManager.registerGnssStatusCallback(callback, null) // Use null for handler to use a background thread

        startForegroundService(serviceIntent)
    }

    @SuppressLint("MissingPermission", "InlinedApi")
    fun requestPermissions() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    startLocationUpdates()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    requestPermissions()
                } else -> {
//                    Log.i("Permissions", permissions.toString());
//                    Log.e("FINNISING", "LOL")
//                requestPermissions();
//                    finish();
                }
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

}



@Composable
fun NeoAnalogSpeedometerScreen(
    currentSpeedKmh: Float,
    maxSpeedKmh: Float,
    sateliteCount: Int,
    avgSpeedKmh: Float,
    distanceKm: Float,
    isTripActive: Boolean,
    onResetClick: () -> Unit,
    onSettingsClick: () -> Unit,
    accuracy: Float,
) {
    val gaugeMaxKmh = 200f

    Surface (
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1E1E) // Dark Background
    ) {
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.padding(top = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "$sateliteCount connected",
                    modifier = Modifier.align(Alignment.CenterVertically),
                    fontSize = 16.sp,
                    color = Color.White,
                )
            IconButton (
                onClick = onSettingsClick,
//                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Max: ${maxSpeedKmh.roundToInt()} km/h",
                color = Color(0xFFE63946), // Accent
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Adjust size of gauge
                    .aspectRatio(1f)
            ) {
                SpeedGauge(currentSpeed = currentSpeedKmh, maxSpeed = gaugeMaxKmh)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f", currentSpeedKmh),
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "km/h",
                        color = Color.LightGray,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Avg: ${String.format("%.1f", avgSpeedKmh)} km/h", color = Color.LightGray, fontSize = 16.sp)
                Text("Dist: ${String.format("%.1f", distanceKm)} km", color = Color.LightGray, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes button to bottom

            Button (
                onClick = onResetClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC22828))
            ) {
                Text("Reset", color = Color(0xFFE0E0E0)) // 0xFF1E1E1E  if (isTripActive) "Stop Trip" else "Start Trip",
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SpeedGauge(currentSpeed: Float, maxSpeed: Float, modifier: Modifier = Modifier) {
    val sweepAngle = (currentSpeed / maxSpeed).coerceIn(0f, 1f) * 300f // Max sweep 300 degrees
    val startAngle = -240f // Start from bottom-left like a gauge

    val gaugeBackgroundColor = Color(0xFF404040)
    val gaugeProgressColor = Color(0xFF00E676) // Green, could be a gradient

    Canvas (modifier = modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * 0.15f // Thickness of the gauge
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        // Background Arc
        drawArc(
            color = gaugeBackgroundColor,
            startAngle = startAngle,
            sweepAngle = 300f, // Full potential arc
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            size = Size(diameter, diameter),
            topLeft = topLeft
        )

        if (sweepAngle > 0) {
            drawArc(
                color = gaugeProgressColor, // Implement gradient for fancier look
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(diameter, diameter),
                topLeft = topLeft
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DefaultPreview() {

    var isTripActive by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(110.3f) } // Sample data
    // In a real app, these would come from a ViewModel or Location Service

    NeoAnalogSpeedometerScreen(
        currentSpeedKmh = currentSpeed,
        maxSpeedKmh = 120f,
        avgSpeedKmh = 80f,
        distanceKm = 25.5f,
        isTripActive = isTripActive,
        onResetClick = {
            isTripActive = !isTripActive
            // Simulate speed change for preview
            if(isTripActive) currentSpeed = 75f else currentSpeed = 0f
        },
        onSettingsClick = {},
        sateliteCount = 6,
        accuracy = 10.344f,
    )
}
