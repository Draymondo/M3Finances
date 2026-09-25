package com.naveenapps.expensemanager.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * PRIV-001 : aucun contenu brut de notification financière ne doit atteindre le logger.
 *
 * Le message émis par [MobileMoneyNotificationListenerService] lors d'une interception est
 * construit par [MobileMoneyNotificationListenerService.interceptedNotificationLogMessage],
 * qui ne reçoit du texte de la notification que sa longueur. Ce test garantit qu'un échantillon
 * de notification sensible (montant, solde, numéro de téléphone, commerçant, nom, référence de
 * transaction) n'apparaît jamais dans le message journalisé.
 *
 * S'exécute sans Robolectric : seul le companion du service est chargé, les appels `Log` sont
 * de toute façon neutralisés par `testOptions.unitTests.isReturnDefaultValues` du module app.
 */
class MobileMoneyNotificationListenerServiceTest {

    @Test
    fun `intercepted notification log never contains the raw notification content`() {
        val message = MobileMoneyNotificationListenerService.interceptedNotificationLogMessage(
            packageName = WAVE_PACKAGE,
            fullText = SENSITIVE_NOTIFICATION,
        )

        // Le message reste utile au diagnostic : paquet source et taille du texte.
        assertThat(message).contains(WAVE_PACKAGE)
        assertThat(message).contains("textLength=${SENSITIVE_NOTIFICATION.length}")

        // Aucune donnée sensible de la notification ne doit y figurer.
        SENSITIVE_SNIPPETS.forEach { snippet ->
            assertThat(message).doesNotContain(snippet)
        }
    }

    @Test
    fun `intercepted notification log does not grow with notification content`() {
        val shortMessage = MobileMoneyNotificationListenerService.interceptedNotificationLogMessage(
            packageName = WAVE_PACKAGE,
            fullText = "Wave",
        )
        val longMessage = MobileMoneyNotificationListenerService.interceptedNotificationLogMessage(
            packageName = WAVE_PACKAGE,
            fullText = SENSITIVE_NOTIFICATION,
        )

        // Le format est exactement le suivant : aucun contenu du texte ne peut s'y glisser,
        // seule la longueur change quand le texte grossit.
        assertThat(shortMessage)
            .isEqualTo("Intercepted notification from $WAVE_PACKAGE (textLength=4)")
        assertThat(longMessage)
            .isEqualTo(
                "Intercepted notification from $WAVE_PACKAGE (textLength=${SENSITIVE_NOTIFICATION.length})",
            )
    }

    companion object {
        private const val WAVE_PACKAGE = "com.wave.personal"

        /** Échantillon représentatif d'une notification mobile money réellement sensible. */
        private const val SENSITIVE_NOTIFICATION =
            "Wave\n" +
                "Vous avez reçu 125 000 FCFA de Jean Konan\n" +
                "Vers le +225 07 08 09 10 11\n" +
                "Solde : 430 500 FCFA\n" +
                "Référence : WAVE-987654321"

        private val SENSITIVE_SNIPPETS = listOf(
            "125 000",
            "FCFA",
            "+225",
            "07 08 09 10 11",
            "Jean Konan",
            "430 500",
            "WAVE-987654321",
            "Solde",
            "Référence",
            "reçu",
        )
    }
}
