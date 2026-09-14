package com.naveenapps.expensemanager.core.domain.usecase.currencyconverter

import com.naveenapps.expensemanager.core.model.CurrencyConversionRates
import com.naveenapps.expensemanager.core.model.Resource

/**
 * Conversion purement locale — aucun appel réseau, [rates] doit déjà avoir été récupéré (voir
 * [RefreshConversionRatesUseCase] / [GetCachedConversionRatesUseCase]) pour la devise source
 * correspondant à [rates.baseCode].
 *
 * `résultat = montant * rates[deviseCible]`, puisque [rates] est toujours exprimé relativement
 * à sa propre devise source — pas de passage par une devise pivot nécessaire.
 */
class ConvertCurrencyUseCase {

    operator fun invoke(
        amount: Double,
        toCode: String,
        rates: CurrencyConversionRates,
    ): Resource<Double> {
        if (amount <= 0.0) {
            return Resource.Error(Exception("Le montant doit être supérieur à 0"))
        }

        val rate = rates.rates[toCode]
            ?: if (toCode == rates.baseCode) 1.0 else null

        return if (rate == null) {
            Resource.Error(Exception("Taux indisponible pour cette devise — réessaie de rafraîchir"))
        } else {
            Resource.Success(amount * rate)
        }
    }
}
