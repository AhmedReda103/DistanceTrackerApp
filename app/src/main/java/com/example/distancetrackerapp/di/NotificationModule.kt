package com.example.distancetrackerapp.di

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.distancetrackerapp.ui.MainActivity
import com.example.distancetrackerapp.R
import com.example.distancetrackerapp.utils.Constants.ACTION_NAVIGATE_TO_MAPS_FRAGMENT
import com.example.distancetrackerapp.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetrackerapp.utils.Constants.PENDING_INTENT_REQUEST_CODE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object NotificationModule {


    @ServiceScoped
    @Provides
    fun providePendingIntent(@ApplicationContext context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                this.action =ACTION_NAVIGATE_TO_MAPS_FRAGMENT
            }, PendingIntent.FLAG_MUTABLE
        )
    }

    @ServiceScoped
    @Provides
    fun provideNotificationBuilder(@ApplicationContext context: Context , pendingIntent: PendingIntent): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_run)
            .setContentIntent(pendingIntent)
    }

    @Provides
    @ServiceScoped
    fun provideNotificationManager(@ApplicationContext context: Context) : NotificationManager{
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }



}














