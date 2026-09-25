package com.naveenapps.expensemanager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Indique si une connexion réseau utilisable est disponible **à l'instant du traitement**.
 *
 * Volontairement petit et testable : l'implémentation Android est isolée derrière l'interface,
 * de sorte que les tests du worker puissent simuler la présence ou l'absence de réseau.
 *
 * Une connexion disponible ne garantit évidemment pas que Gemini répondra : les erreurs
 * réseau/timeout restent soumises à la politique de retry du chantier 3G.
 *
 * Implémentation basée sur [ConnectivityManager.getNetworkCapabilities] (API 21+), cohérente
 * avec le `minSdk` du projet. Aucune permission supplémentaire n'est requise :
 * `ACCESS_NETWORK_STATE` est déjà déclarée dans le manifeste.
 */
interface NetworkMonitor {
    fun isNetworkAvailable(): Boolean
}

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override fun isNetworkAvailable(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
