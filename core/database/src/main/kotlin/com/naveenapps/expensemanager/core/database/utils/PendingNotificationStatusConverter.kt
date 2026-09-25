package com.naveenapps.expensemanager.core.database.utils

import androidx.room.TypeConverter
import com.naveenapps.expensemanager.core.model.PendingNotificationStatus

/**
 * Convertisseur de statut de la file offline. Le statut est persisté **par nom** (et non par
 * ordinal) : la file est locale et éphémère, un ajout futur d'état ne doit jamais décaler un
 * statut déjà enregistré.
 */
class PendingNotificationStatusConverter {
    @TypeConverter
    fun fromStatus(status: PendingNotificationStatus?): String? = status?.name

    @TypeConverter
    fun toStatus(value: String?): PendingNotificationStatus? =
        value?.let { raw -> PendingNotificationStatus.entries.firstOrNull { it.name == raw } }
}
