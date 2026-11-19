package com.naveenapps.expensemanager.feature.debt.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.debt.DebtUiModel
import com.naveenapps.expensemanager.core.model.Amount
import java.util.Date

/**
 * Toutes les dettes d'une même personne, additionnées.
 *
 * [key] est le nom normalisé (espaces retirés, minuscules) : c'est la clé de regroupement, donc
 * « Ali » et « ali » forment une seule carte. [personName] garde une graphie lisible pour l'affichage.
 */
@Stable
data class PersonDebtGroup(
    val key: String,
    val personName: String,
    val debts: List<DebtUiModel>,
    /** Somme des restes dus, déjà formatée dans la devise de l'application. */
    val total: Amount?,
    val hasOverdue: Boolean,
    val nextDueDate: Date?,
)

/**
 * Écran « Prêts & dettes » en deux sections : ce qu'on nous doit, ce que nous devons.
 *
 * Les dettes soldées (ou dont le reste dû est tombé à zéro) sortent des deux sections et des
 * totaux : elles atterrissent dans [settled], replié par défaut.
 */
@Stable
data class DebtListState(
    val isLoading: Boolean = true,
    val debtors: List<PersonDebtGroup> = emptyList(),
    val creditors: List<PersonDebtGroup> = emptyList(),
    val settled: List<PersonDebtGroup> = emptyList(),
    val debtorsTotal: Amount? = null,
    val creditorsTotal: Amount? = null,
) {

    val isEmpty: Boolean
        get() = debtors.isEmpty() && creditors.isEmpty() && settled.isEmpty()

    /** Nombre de dettes encore ouvertes, toutes personnes confondues. */
    val openDebtsCount: Int
        get() = debtors.sumOf { it.debts.size } + creditors.sumOf { it.debts.size }
}
