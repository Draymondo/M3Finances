package com.naveenapps.expensemanager.core.model

import java.util.Date

/**
 * Taux de change mis en cache pour une devise source ("base") donnée — récupérés via
 * exchangerate-api.com. [rates] contient un taux par devise cible (ex: "EUR" -> 0.92), déjà
 * exprimé relativement à [baseCode], donc `montant * rates[deviseCible]` suffit pour convertir
 * sans passage par une devise pivot.
 */
data class CurrencyConversionRates(
    val baseCode: String,
    val rates: Map<String, Double>,
    val lastUpdated: Date,
)
