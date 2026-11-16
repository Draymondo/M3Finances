package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.TransactionType
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Test

class WaveNotificationParserTest {
    private val parser = WaveNotificationParser()
    private val zone = TimeZone.getTimeZone("UTC")

    @Test
    fun `payment parses amount merchant date time and balance without reference`() {
        val result = recognized("Paiement réussi!\n\nVous avez payé 1.000F à Au Gabi Choco le 29 Aug 16h03.\nSolde Wave: 27.744F Avec UBA", postTime(2026, Calendar.AUGUST, 29, 16, 3))
        assertThat(result.amount).isEqualTo(1000.0)
        assertThat(result.merchant).isEqualTo("Au Gabi Choco")
        assertThat(result.transactionType).isEqualTo(TransactionType.EXPENSE)
        assertThat(result.balance).isEqualTo(27744.0)
        assertThat(result.providerReference).isNull()
    }

    @Test
    fun `deposit parses amount merchant date time balance and reference`() {
        val result = recognized("Dépôt réussi!\n\nVous avez déposé 3.000F chez KAR SERVICES le 29/08/2026 à 15h56.\nSolde Wave: 28.744F Avec UBA ATX2KPDL2E3EBUMPQPR")
        assertThat(result.amount).isEqualTo(3000.0)
        assertThat(result.merchant).isEqualTo("KAR SERVICES")
        assertThat(result.transactionType).isEqualTo(TransactionType.INCOME)
        assertThat(result.providerReference).isEqualTo("ATX2KPDL2E3EBUMPQPR")
    }

    @Test
    fun `withdrawal parses amount merchant date time balance and reference`() {
        val result = recognized("Retrait réussi!\n\nVous avez retiré 5.000F chez KOFFI LUC SERVICES le 29/08/2026 à 15h43.\nSolde Wave: 25.744F Avec UBA ATX2MTHCLGJ4XVMULNC")
        assertThat(result.amount).isEqualTo(5000.0)
        assertThat(result.merchant).isEqualTo("KOFFI LUC SERVICES")
        assertThat(result.transactionType).isEqualTo(TransactionType.EXPENSE)
        assertThat(result.providerReference).isEqualTo("ATX2MTHCLGJ4XVMULNC")
    }

    @Test
    fun `sent transfer parses phone fee balance and reference`() {
        val result = recognized("Transfert réussi!\n\nVous avez envoyé 10F A Raymond A J Djadoo (0502963098)\n29/08/2026 à 00h46\nFrais: 5F\nNouveau solde: 49F\nAvec UBA T7QKVMMGPNOXPOGKG")
        assertThat(result.amount).isEqualTo(10.0)
        assertThat(result.merchant).isEqualTo("Raymond A J Djadoo")
        assertThat(result.partyPhone).isEqualTo("0502963098")
        assertThat(result.fee).isEqualTo(5.0)
        assertThat(result.transactionType).isEqualTo(TransactionType.TRANSFER)
        assertThat(result.balance).isEqualTo(49.0)
    }

    @Test
    fun `received transfer parses phone balance info and reference`() {
        val result = recognized("Transfert reçu!\n\nVous avez reçu 10F De Djadoo R A Jephte (0102053893)\n29/08/2026 à 00h39\nNouveau solde: 55F\n+infos: 1315\nAvec UBA TTG5VUKEELRYRZLSD")
        assertThat(result.amount).isEqualTo(10.0)
        assertThat(result.merchant).isEqualTo("Djadoo R A Jephte")
        assertThat(result.partyPhone).isEqualTo("0102053893")
        assertThat(result.transactionType).isEqualTo(TransactionType.INCOME)
        assertThat(result.additionalInfo).isEqualTo("1315")
        assertThat(result.providerReference).isEqualTo("TTG5VUKEELRYRZLSD")
    }

    @Test
    fun `negative and ambiguous notifications are not recognized`() {
        val postTime = postTime(2026, Calendar.AUGUST, 29, 16, 3)
        val invalidTexts = listOf(
            "Paiement réussi! Vous avez payé 1.000F à Boutique le 29 Aug 16h03.",
            "Dépôt réussi! Vous avez déposé 3.000F chez Agent.",
            "Retrait réussi! Vous avez retiré 5.000F chez Agent le 29/08/2026 à 15h43.",
            "Transfert réussi! Vous avez envoyé 10F A Raymond (0502963098) 29/08/2026 à 00h46",
            "Opération Wave inconnue",
        )
        invalidTexts.forEach { text ->
            assertThat(parser.parse(text, postTime, zone)).isInstanceOf(WaveParseResult.NotRecognized::class.java)
        }
        assertThat(parser.parse(
            "Paiement réussi! Vous avez payé 1.000F à Boutique le 29 Aug 16h03. Solde Wave: 27.744F",
            postTime = null,
            zone = zone,
        )).isInstanceOf(WaveParseResult.NotRecognized::class.java)
    }

    @Test
    fun `small and large amounts are supported`() {
        val smallResult = parser.parse("Paiement réussi! Vous avez payé 10F à Boutique le 29/08/2026 à 15h43. Solde Wave: 27F", postTime(2026, Calendar.AUGUST, 29, 15, 0), zone)
        assertThat(smallResult).isInstanceOf(WaveParseResult.Recognized::class.java)
        val small = (smallResult as WaveParseResult.Recognized).transaction
        val large = recognized("Paiement réussi! Vous avez payé 1.234.567F à Boutique le 29/08/2026 à 15h43. Solde Wave: 27F")
        assertThat(small.amount).isEqualTo(10.0)
        assertThat(large.amount).isEqualTo(1234567.0)
    }

    @Test
    fun `headings without exclamation mark are recognized`() {
        val result = recognized("Paiement réussi\n\nVous avez payé 1.000F à Boutique le 29 Aug 16h03.\nSolde Wave: 27.744F")
        assertThat(result.amount).isEqualTo(1000.0)
        assertThat(result.transactionType).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun `amounts with no-break space thousands separator and F CFA suffix are recognized`() {
        val result = recognized("Paiement réussi!\n\nVous avez payé 1\u00A0000\u00A0F\u00A0CFA à Boutique le 29 Aug 16h03.\nSolde Wave: 27\u00A0744\u00A0F\u00A0CFA")
        assertThat(result.amount).isEqualTo(1000.0)
        assertThat(result.balance).isEqualTo(27744.0)
    }

    @Test
    fun `amounts with narrow no-break space thousands separator are recognized`() {
        val result = recognized("Paiement réussi!\n\nVous avez payé 1\u202F000\u202FF à Boutique le 29 Aug 16h03.\nSolde Wave: 27\u202F744\u202FF")
        assertThat(result.amount).isEqualTo(1000.0)
        assertThat(result.balance).isEqualTo(27744.0)
    }

    @Test
    fun `transfer with international phone prefix is recognized`() {
        val result = recognized("Transfert reçu!\n\nVous avez reçu 10F De Raymond (+225 05 02 96 30 98)\n29/08/2026 à 00h46\nNouveau solde: 49F")
        assertThat(result.partyPhone).isEqualTo("2250502963098")
        assertThat(result.transactionType).isEqualTo(TransactionType.INCOME)
    }

    @Test
    fun `generic solde label and dash numeric date are recognized`() {
        val result = recognized("Dépôt réussi!\n\nVous avez déposé 3.000F chez KAR SERVICES le 29-08-2026 à 15h56.\nSolde: 28.744F")
        assertThat(result.amount).isEqualTo(3000.0)
        assertThat(result.balance).isEqualTo(28744.0)
    }

    @Test
    fun `nouveau solde wave label is recognized`() {
        val result = recognized("Retrait réussi!\n\nVous avez retiré 5.000F chez Agent le 29/08/2026 à 15h43.\nNouveau solde Wave: 20.000F")
        assertThat(result.balance).isEqualTo(20000.0)
    }

    @Test
    fun `decimal amounts are parsed with french convention`() {
        val result = recognized("Paiement réussi!\n\nVous avez payé 1.000,50F à Boutique le 29 Aug 16h03.\nSolde Wave: 27.744F")
        assertThat(result.amount).isEqualTo(1000.5)
    }

    @Test
    fun `single dot with one or two fraction digits is a decimal`() {
        val result = recognized("Paiement réussi!\n\nVous avez payé 1.50F à Boutique le 29 Aug 16h03.\nSolde Wave: 27.744F")
        assertThat(result.amount).isEqualTo(1.5)
    }

    private fun recognized(text: String, postTime: Long? = postTime(2026, Calendar.AUGUST, 29, 15, 0)): ParsedWaveTransaction {
        val result = parser.parse(text, postTime, zone)
        assertThat(result).isInstanceOf(WaveParseResult.Recognized::class.java)
        return (result as WaveParseResult.Recognized).transaction
    }

    private fun postTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(zone).apply { clear(); set(year, month, day, hour, minute, 0) }.timeInMillis
}
