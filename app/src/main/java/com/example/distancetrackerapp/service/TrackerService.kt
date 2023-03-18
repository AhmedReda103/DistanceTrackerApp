package com.example.distancetrackerapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetrackerapp.ui.maps.MapUtil
import com.example.distancetrackerapp.utils.Constants
import com.example.distancetrackerapp.utils.Constants.ACTION_SERVICE_STARTED
import com.example.distancetrackerapp.utils.Constants.ACTION_SERVICE_STOP
import com.example.distancetrackerapp.utils.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetrackerapp.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetrackerapp.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.distancetrackerapp.utils.Constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackerService : LifecycleService() {

    companion object {
        val started = MutableLiveData<Boolean>()
        val locationList = MutableLiveData<MutableList<LatLng>>()
        val startTime = MutableLiveData<Long>()
        val stopTime = MutableLiveData<Long>()
    }

    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    val locationCallBack = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.let { locations ->
                for (location in locations) {
                    updateLocation(location)
                    updateNotificationPeriodically()
                }
            }
        }
    }

    private fun updateNotificationPeriodically() {
        notification.apply {
            setContentTitle("Distance Travelled")
                setContentText(locationList.value?.let {
                    MapUtil.calculateDistance(it)
                })
        }
        notificationManager.notify(NOTIFICATION_ID , notification.build())
    }

    private fun updateLocation(location: Location) {
        val newLocation = LatLng(location.latitude, location.longitude)
        locationList.value?.apply {
            add(newLocation)
            locationList.postValue(this)
        }
    }


    private fun setInitialValue() {
        started.postValue(false)
        locationList.postValue(mutableListOf())
        startTime.postValue(0L)
        stopTime.postValue(0L)
    }

    override fun onCreate() {
        setInitialValue()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SERVICE_STARTED -> {
                    started.postValue(true)
                    startForegroundService()
                    startLocationUpdate()
                }
                ACTION_SERVICE_STOP -> {
                    started.postValue(false)
                    stopForegroundService()
                }
                else -> {}
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopForegroundService() {
        removeFusedLocationProvider()
    }

    private fun removeFusedLocationProvider() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification.build())
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        val locationRequest: LocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallBack,
            Looper.getMainLooper()
        )

        startTime.postValue(System.currentTimeMillis())

    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


}