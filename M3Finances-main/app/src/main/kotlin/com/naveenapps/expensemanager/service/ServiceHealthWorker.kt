package com.naveenapps.expensemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.MainActivity
import com.naveenapps.expensemanager.R

class ServiceHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ServiceHealthWorker", "Checking NotificationListenerService health...")
        val context = applicationContext
        
        val isPermissionGranted = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)

        if (!isPermissionGranted) {
            Log.e("ServiceHealthWorker", "Permission disabled. Alerting user.")
            alertUser(context)
            return Result.success()
        }

        Log.i("ServiceHealthWorker", "Permission is granted. Performing preventative restart.")
        try {
            val pm = context.packageManager
            val component = ComponentName(context, MobileMoneyNotificationListenerService::class.java)
            
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e("ServiceHealthWorker", "Failed to restart component: ${e.message}")
        }

        return Result.success()
    }

    private fun alertUser(context: Context) {
        val channelId = "health_check_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Santé de l'application",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        val pendingIntent = try {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } catch (e: Exception) {
            val fallbackIntent = Intent(context, MainActivity::class.java)
            PendingIntent.getActivity(context, 0, fallbackIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // We use android.R.drawable.ic_dialog_alert as a safe fallback icon
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Attention : Transactions perdues ?")
            .setContentText("L'accès aux notifications a été coupé par le système. Cliquez ici pour réparer.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(8888, notification)
    }
}

