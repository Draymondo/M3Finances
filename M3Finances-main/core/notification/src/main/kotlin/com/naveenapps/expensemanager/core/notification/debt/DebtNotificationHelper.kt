package com.naveenapps.expensemanager.core.notification.debt

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
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.isLent
import com.naveenapps.expensemanager.core.notification.DESTINATION_CLASS
import com.naveenapps.expensemanager.core.notification.R

private const val CHANNEL_DEBT_REMINDERS = "DebtReminders"

/**
 * Separate from `NotificationScheduler`/`NotificationChannelId.CHANNEL_GENERAL` on purpose (see
 * `DebtReminderScheduler`) — debt reminders have their own on/off switch at the OS level (their
 * own channel) and aren't gated behind the general daily-reminder toggle, so a person who's
 * turned that off in Settings still gets warned about money owed.
 */
internal fun canShowDebtNotification(context: Context): Boolean {
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

internal fun showDebtNotification(
    context: Context,
    requestCode: Int,
    title: String,
    content: String,
) {
    if (!canShowDebtNotification(context)) return

    createDebtReminderChannelIfRequired(context)

    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notificationIntent = Intent(context, Class.forName(DESTINATION_CLASS))
    notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

    val pendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_DEBT_REMINDERS)
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

private fun createDebtReminderChannelIfRequired(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_DEBT_REMINDERS,
        CHANNEL_DEBT_REMINDERS,
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.debt_reminder_channel_description)
        lightColor = Color.BLUE
    }
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}

/** "Jean vous doit 50,00 €" / "Vous devez 50,00 € à Jean", depending on [Debt.direction]. */
internal fun debtNotificationContent(context: Context, debt: Debt, formattedAmount: String): String {
    return if (debt.direction.isLent()) {
        context.getString(R.string.debt_reminder_content_lent, debt.personName, formattedAmount)
    } else {
        context.getString(R.string.debt_reminder_content_borrowed, debt.personName, formattedAmount)
    }
}
