package com.jaw0r3k.speedometer

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class LocalizationDataManager {
    companion object {
        val speedLiveData: MutableLiveData<Float> = MutableLiveData<Float>(0f)
        val maxSpeedLiveData: MutableLiveData<Float> = MutableLiveData<Float>(0f)
        val averageSpeedLiveData: MutableLiveData<Float> = MutableLiveData<Float>(0f)
        val accuracyLiveData: MutableLiveData<Float> = MutableLiveData<Float>(0f)
        val distanceTravelledLiveData: MutableLiveData<Double> = MutableLiveData<Double>(0f.toDouble())
        private var locations: MutableList<Location> = mutableListOf()
        private lateinit var previousLocation: Location;
        private var weightedSpeedSum = 0.0
        private var totalDurationSeconds = 0.0
        private var isFirstSpeedUpdate = true

        fun updateSpeed(location: Location) {
            locations.add(location);

            var speed =  location.speed * 3.6f;
            var currentTimestamp = location.time;
            speedLiveData.postValue(speed);

            if (maxSpeedLiveData.value!! < speed) {
                maxSpeedLiveData.postValue(speed);
            }

            if(isFirstSpeedUpdate) {
                previousLocation = location;
                isFirstSpeedUpdate = false
                return
            }

            val deltaTimeMillis = currentTimestamp - previousLocation.time
            if (deltaTimeMillis <= 0) {
                previousLocation = location;
                return
            }

            val deltaTimeSeconds = deltaTimeMillis / 1000.0

            weightedSpeedSum += previousLocation.speed * deltaTimeSeconds
            totalDurationSeconds += deltaTimeSeconds

            averageSpeedLiveData.postValue((weightedSpeedSum / totalDurationSeconds).toFloat())

            previousLocation = location;

            distanceTravelledLiveData.value = distanceTravelledLiveData.value?.plus(
                calculateHaversineDistance(previousLocation, location)
            )

            Log.i("Average", averageSpeedLiveData.value.toString());
        }

        fun calculateHaversineDistance(start: Location, end: Location): Double {
            val earthRadiusKm = 6371.0

            val dLat = Math.toRadians(end.latitude - start.latitude)
            val dLon = Math.toRadians(end.longitude - start.longitude)

            val lat1 = Math.toRadians(start.latitude)
            val lat2 = Math.toRadians(end.latitude)

            val a = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * cos(lat1) * cos(lat2)
            val c = 2 * asin(sqrt(a))

            return earthRadiusKm * c
        }


        fun updateAccuracy(accuracy: Float) {
            accuracyLiveData.value = accuracy;
        }

        fun reset() {

            weightedSpeedSum = 0.0
            totalDurationSeconds = 0.0
            isFirstSpeedUpdate = true;
            speedLiveData.postValue(0f);
            maxSpeedLiveData.postValue(0f);
        }

    }

}