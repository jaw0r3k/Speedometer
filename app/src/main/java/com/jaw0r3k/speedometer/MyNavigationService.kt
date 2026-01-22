package com.jaw0r3k.speedometer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MyNavigationService : Service() {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        Log.i("Service", "Creating")
        createNotificationChannel()

        val stopSelf = Intent(this, MyNavigationService::class.java)
        stopSelf.action = "STOP_SERVICE_ACTION"
        val stopServicePendingIntent = PendingIntent.getService(
            this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "NAVIGATION_SERVICE_CHANNEL")
            .setContentTitle("Speedometer Active")
            .setContentText("Speedometer is running in the background")
            .setSmallIcon(R.drawable.ic_stat_name) // **IMPORTANT: Create this drawable!**
//            .setContentIntent(Intent(baseContext, MainActivity::class::java))
            .setOngoing(true) // Makes the notification non-dismissable by swiping
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For visibility
             .addAction(R.drawable.baseline_stop_24, "Stop Tracking", stopServicePendingIntent)
            .build()
        ServiceCompat.startForeground(this, 100 ,notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 400)
            .setMinUpdateIntervalMillis(1500)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                locationResult.lastLocation?.let {
                    Log.i("Accuracy", it.accuracy.toString())
                    Log.i("Accuracy speed", it.speedAccuracyMetersPerSecond.toString())
                    LocalizationDataManager.updateAccuracy(it.speedAccuracyMetersPerSecond);
                    if(it.speedAccuracyMetersPerSecond < 1f && it.accuracy < 20) {
                        Log.i("Speed", (it.speed * 3.6f).toString())
                        LocalizationDataManager.updateSpeed(it)
                    }
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)
//                Log.i("Availability", p0.isLocationAvailable.toString())
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE_ACTION") {
            stopSelf() // Stops the service
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun createNotificationChannel(){
        val notificationChannel = NotificationChannel("NAVIGATION_SERVICE_CHANNEL", "NAVIGATION_SERVICE_CHANNEL",
            NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)
    }


    override fun onBind(intent: Intent): IBinder? {
        Log.i("Service", "Binding")
        return null
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.i("Service", "Destroying")
    }

}