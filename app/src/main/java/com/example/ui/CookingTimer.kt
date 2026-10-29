package com.example.ui

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class CookingTimer : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel("cooking", "Cooking timers",NotificationManager.IMPORTANCE_HIGH))
        val open=PendingIntent.getActivity(context,71,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(71,NotificationCompat.Builder(context,"cooking").setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to check your cooking").setContentText(intent.getStringExtra("dish")?.take(100)?:"Your cooking timer has ended.")
            .setContentIntent(open).setAutoCancel(true).build())
    }
    companion object {
        fun schedule(context: Context, endsAt: Long?, dish: String) {
            val pending=PendingIntent.getBroadcast(context,71,Intent(context,CookingTimer::class.java).putExtra("dish",dish),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val manager=context.getSystemService(AlarmManager::class.java)
            manager.cancel(pending)
            context.getSystemService(NotificationManager::class.java).cancel(71)
            // shortcut: inexact background reminders may be delayed by Android; the persisted in-app deadline stays accurate.
            if(endsAt!=null) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,endsAt,pending)
        }
    }
}
