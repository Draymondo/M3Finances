package com.naveenapps.expensemanager.core.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeminiRepositoryImplTest {

    @Test
    fun `explicit fee in notification should be kept exactly`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Transfert réussi! Vous avez envoyé 10F. Frais: 5F",
            parsedType = "TRANSFER",
            parsedFee = 5.0,
            amount = 10.0,
        )

        assertThat(fee).isEqualTo(5.0)
    }

    @Test
    fun `transfer with no explicit fee and envoyer keyword should use one percent fallback`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Transfert réussi! Vous avez envoyé 1000F à Raymond",
            parsedType = "TRANSFER",
            parsedFee = null,
            amount = 1000.0,
        )

        assertThat(fee).isEqualTo(10.0)
    }

    @Test
    fun `non transfer payment should not create a fee automatically`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Paiement accepté. Vous avez payé 1000F au magasin",
            parsedType = "EXPENSE",
            parsedFee = null,
            amount = 1000.0,
        )

        assertThat(fee).isNull()
    }

    @Test
    fun `explicit zero fee should not create a fee entry`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Transfert réussi! Frais: 0F",
            parsedType = "TRANSFER",
            parsedFee = 0.0,
            amount = 1000.0,
        )

        assertThat(fee).isNull()
    }
}
