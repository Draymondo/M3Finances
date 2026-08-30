package com.naveenapps.expensemanager.core.notification.budget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.naveenapps.expensemanager.core.notification.DESTINATION_CLASS
import com.naveenapps.expensemanager.core.notification.R

private const val CHANNEL_BUDGET_ALERTS = "BudgetAlerts"

/** Own channel, own on/off switch at the OS level — same reasoning as
 * `canShowDebtNotification`: budget alerts aren't gated behind the general daily-reminder
 * toggle in Settings. */
internal fun canShowBudgetNotification(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    return true
}

internal fun showBudgetNotification(
    context: Context,
    requestCode: Int,
    title: String,
    content: String,
) {
    if (!canShowBudgetNotification(context)) return

    createBudgetAlertChannelIfRequired(context)

    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notificationIntent = Intent(context, Class.forName(DESTINATION_CLASS))
    notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

    val pendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
        .setContentTitle(title)
        .setContentText(content)
        .setAutoCancel(true)
        .setSound(alarmSound)
        .setSmallIcon(R.drawable.account_balance_wallet)
        .setContentIntent(pendingIntent)
        .build()

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(requestCode, notification)
}

private fun createBudgetAlertChannelIfRequired(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_BUDGET_ALERTS,
        CHANNEL_BUDGET_ALERTS,
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.budget_alert_channel_description)
        lightColor = Color.BLUE
    }
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}
