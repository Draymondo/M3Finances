package com.naveenapps.expensemanager.core.model

/**
 * Unité de sortie du résultat de la conversion prix → temps de travail.
 * L'unité "année" est explicitement hors scope pour cette version.
 */
enum class WorkTimeUnit {
    MINUTE,
    HOUR,
    DAY,
    MONTH,
}

