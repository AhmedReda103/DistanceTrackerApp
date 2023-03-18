package com.example.distancetrackerapp.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog

object Constants {

    const val ACTION_SERVICE_STARTED = "ACTION_SERVICE_STARTED"
    const val ACTION_SERVICE_STOP    =  "ACTION_SERVICE_STOP"

    const val NOTIFICATION_CHANNEL_ID = "tracker_notification_id"
    const val NOTIFICATION_CHANNEL_NAME = "tracker_notification"


    const val PENDING_INTENT_REQUEST_CODE = 99
    const val ACTION_NAVIGATE_TO_MAPS_FRAGMENT = "ACTION_NAVIGATE_TO_MAPS_FRAGMENT"

    const val NOTIFICATION_ID = 3

    val LOCATION_UPDATE_INTERVAL = 4000L
    val LOCATION_FASTEST_UPDATE_INTERVAL = 2000L


    fun showRationaleDialog(title :String , message :String , context : Context) {
        val builder : AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){ dialoge ,_ ->
                dialoge.dismiss()
            }
        builder.create().show()
    }

}