package com.naveenapps.expensemanager.core.common.utils

import com.naveenapps.expensemanager.core.common.R

/**
 * Couleur du texte d'un montant.
 *
 * Un solde **nul** n'est ni une bonne ni une mauvaise nouvelle : il est rendu
 * en gris muet (voir la maquette « comptes à 0 FCFA en gris ») au lieu du vert
 * historique qui mettait en avant de l'argent qui n'existe pas.
 */
fun Double.getAmountTextColor() = when {
    this < -ZERO_AMOUNT_EPSILON -> R.color.red_500
    this > ZERO_AMOUNT_EPSILON -> R.color.green_500
    else -> R.color.gray_500
}

private const val ZERO_AMOUNT_EPSILON = 0.005

fun Int.toColorString(): String {
    return String.format("#%06X", 0xFFFFFF and this)
}
