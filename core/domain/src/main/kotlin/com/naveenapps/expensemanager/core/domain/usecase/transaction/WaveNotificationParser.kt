package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.naveenapps.expensemanager.core.model.TransactionType
import java.text.Normalizer
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Parser déterministe et fail-closed des notifications Wave connues. */
class WaveNotificationParser {
    fun parse(text: String, postTime: Long? = null, zone: TimeZone = TimeZone.getDefault()): WaveParseResult {
        if (text.isBlank()) return WaveParseResult.NotRecognized("Texte vide")
        val receivedTransfer = heading(text, "transfert", "re[çc]u")
        val isTransfer = heading(text, "transfert", "r[ée]ussi") || receivedTransfer
        val type = when {
            heading(text, "paiement", "r[ée]ussi") -> TransactionType.EXPENSE
            heading(text, "d[ée]p[ôo]t", "r[ée]ussi") -> TransactionType.INCOME
            heading(text, "retrait", "r[ée]ussi") -> TransactionType.EXPENSE
            receivedTransfer -> TransactionType.INCOME
            isTransfer -> TransactionType.TRANSFER
            else -> return WaveParseResult.NotRecognized("Format Wave non reconnu")
        }
        val match = (if (isTransfer) transferRegex(text) else operationRegex(text))
            ?.find(text) ?: return WaveParseResult.NotRecognized("Corps d'opération absent")
        val amount = amount(match.groups["amount"]?.value)
            ?: return WaveParseResult.NotRecognized("Montant invalide")
        val date = date(match.groups["date"]?.value, match.groups["time"]?.value, postTime, zone)
            ?: return WaveParseResult.NotRecognized("Date invalide")
        val balance = amount(BALANCE.find(text)?.groups?.get("balance")?.value)
            ?: return WaveParseResult.NotRecognized("Solde absent ou invalide")
        val fee = if (FEE_MARKER.containsMatchIn(text)) {
            amount(FEE.find(text)?.groups?.get("fee")?.value)
                ?: return WaveParseResult.NotRecognized("Frais invalides")
        } else null
        val phone = if (isTransfer) {
            match.groups["phone"]?.value?.filter(Char::isDigit)?.takeIf { it.length in 6..15 }
        } else {
            null
        }
        val party = if (isTransfer) {
            match.groups["party"]?.value?.trim()?.trim('.', ',', ';', ':', '!', '?')
        } else {
            match.groups["merchant"]?.value?.trim()?.trim('.', ',', ';', ':', '!', '?')
        }
        if (party.isNullOrBlank() || (isTransfer && phone == null)) {
            return WaveParseResult.NotRecognized("Tiers incomplet")
        }
        val reference = REFERENCE.find(text)?.groups?.get("reference")?.value?.trim()
            ?.takeIf { it.isNotBlank() }
        val info = INFO.find(text)?.groups?.get("info")?.value?.trim()?.takeIf { it.isNotBlank() }
        return WaveParseResult.Recognized(
            ParsedWaveTransaction(amount, fee, party, phone, date, type, balance, reference, info)
        )
    }

    private fun operationRegex(text: String): Regex? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            "vous avez payé" in lower || "vous avez paye" in lower -> Regex(
                """vous\s+avez\s+pay[ée]\s+(?<amount>$AMOUNT)\s+[àa]\s+(?<merchant>.+?)\s+le\s+(?<date>\d{1,2}(?:\s+[A-Za-zÀ-ÿ.]{2,9}|$DATE_NUM))\s*(?:[àa]\s*)?(?<time>$TIME)""",
                RegexOption.IGNORE_CASE,
            )
            "vous avez déposé" in lower || "vous avez depose" in lower || "vous avez retiré" in lower || "vous avoir retire" in lower -> Regex(
                """vous\s+avez\s+(?<verb>d[ée]pos[ée]|retir[ée])\s+(?<amount>$AMOUNT)\s+chez\s+(?<merchant>.+?)\s+le\s+(?<date>$DATE_NUM)\s+[àa]\s+(?<time>$TIME)""",
                RegexOption.IGNORE_CASE,
            )
            else -> null
        }
    }

    private fun transferRegex(text: String): Regex {
        val received = "reçu" in text.lowercase(Locale.ROOT) || "recu" in text.lowercase(Locale.ROOT)
        return Regex(
            if (received) """vous\s+avez\s+re[çc]u\s+(?<amount>$AMOUNT)\s+de\s+(?<party>.+?)\s*\((?<phone>\+?[\d .-]{5,19})\)\s*(?:\r?\n|,)?\s*(?<date>$DATE_NUM)\s+[àa]\s+(?<time>$TIME)"""
            else """vous\s+avez\s+envoy[ée]\s+(?<amount>$AMOUNT)\s+[àa]\s+(?<party>.+?)\s*\((?<phone>\+?[\d .-]{5,19})\)\s*(?:\r?\n|,)?\s*(?<date>$DATE_NUM)\s+[àa]\s+(?<time>$TIME)""",
            RegexOption.IGNORE_CASE,
        )
    }


    private fun amount(raw: String?): Double? {
        // On ne conserve que chiffres, séparateurs et virgule, puis on normalise selon la
        // convention française (point/espace = milliers, virgule = décimale).
        val cleaned = raw.orEmpty()
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace('\'', ' ')
            .filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }
            .trim()
        if (cleaned.isEmpty()) return null
        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 ->
                if (lastComma > lastDot) {
                    cleaned.replace(".", "").replace(" ", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "").replace(" ", "")
                }
            lastComma >= 0 -> {
                val singleComma = cleaned.count { it == ',' } == 1
                val decimals = cleaned.length - lastComma - 1
                if (singleComma && decimals in 1..2) {
                    cleaned.replace(" ", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "").replace(" ", "")
                }
            }
            lastDot >= 0 -> {
                val dots = cleaned.count { it == '.' }
                val decimals = cleaned.length - lastDot - 1
                if (dots == 1 && decimals in 1..2) {
                    cleaned.replace(" ", "")
                } else {
                    cleaned.replace(".", "").replace(" ", "")
                }
            }
            else -> cleaned.replace(" ", "")
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun date(raw: String?, time: String?, postTime: Long?, zone: TimeZone): Date? {
        val date = raw?.trim().orEmpty()
        val timeValue = time?.trim().orEmpty()
        if (!TIME.matches(timeValue)) return null
        val parts = timeValue.split('h')
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        NUMERIC.matchEntire(date)?.let {
            return createDate(it.groupValues[3].toInt(), it.groupValues[2].toInt() - 1,
                it.groupValues[1].toInt(), hour, minute, zone)
        }
        val dayMonth = DAY_MONTH.matchEntire(date) ?: return null
        val reference = postTime?.takeIf { it > 0 } ?: return null
        val day = dayMonth.groupValues[1].toIntOrNull() ?: return null
        val month = month(dayMonth.groupValues[2]) ?: return null
        val year = Calendar.getInstance(zone).apply { timeInMillis = reference }.get(Calendar.YEAR)
        return (year - 1..year + 1)
            .mapNotNull { createDate(it, month, day, hour, minute, zone) }
            .filter { it.time <= reference + FUTURE_WINDOW && it.time >= reference - PAST_WINDOW }
            .minByOrNull { kotlin.math.abs(it.time - reference) }
    }

    private fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: TimeZone): Date? {
        val calendar = Calendar.getInstance(zone).apply {
            isLenient = false
            clear()
            set(year, month, day, hour, minute, 0)
        }
        if (calendar.get(Calendar.YEAR) != year || calendar.get(Calendar.MONTH) != month ||
            calendar.get(Calendar.DAY_OF_MONTH) != day
        ) return null
        return calendar.time
    }

    private fun month(raw: String): Int? = when (
        Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trimEnd('.')
    ) {
        "jan", "janv", "janvier" -> Calendar.JANUARY
        "fev", "fevr", "fevrier" -> Calendar.FEBRUARY
        "mar", "mars" -> Calendar.MARCH
        "avr", "avril" -> Calendar.APRIL
        "mai" -> Calendar.MAY
        "juin" -> Calendar.JUNE
        "juil", "juillet" -> Calendar.JULY
        "aou", "aout", "aug" -> Calendar.AUGUST
        "sep", "sept", "septembre" -> Calendar.SEPTEMBER
        "oct", "octobre" -> Calendar.OCTOBER
        "nov", "novembre" -> Calendar.NOVEMBER
        "dec", "decembre" -> Calendar.DECEMBER
        else -> null
    }

    private fun heading(text: String, subject: String, status: String): Boolean =
        // Le point d'exclamation final est facultatif : Wave ne le met pas toujours.
        Regex("""\b$subject\s+$status\b\s*!?""", RegexOption.IGNORE_CASE).containsMatchIn(text)

    private companion object {
        // Montant : milliers séparés par point, espace (insécable ou fine), apostrophe ou
        // espace classique ; décimale facultative ; monnaie « F », « F CFA », « FCFA », « CFA ».
        const val AMOUNT = """(?:\d{1,3}(?:[\u00A0\u202F' .]\d{3})+|\d+)(?:[.,]\d{1,2})?[\u00A0\u202F ]*(?:F(?:[\u00A0\u202F ]?CFA)?|FCFA|CFA)\b"""
        const val DATE_NUM = """\d{1,2}[/.-]\d{1,2}[/.-]\d{4}"""
        const val FUTURE_WINDOW = 5 * 60 * 1000L
        const val PAST_WINDOW = 2 * 24 * 60 * 60 * 1000L
        val TIME = Regex("""\d{1,2}h\d{2}""")
        val NUMERIC = Regex("""(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})""")
        val DAY_MONTH = Regex("""(\d{1,2})\s+([A-Za-zÀ-ÿ.]{2,9})""")
        val BALANCE = Regex("""(?:^|\s)(?:nouveau\s+solde(?:\s+wave)?|solde(?:\s+wave|\s+disponible|\s+actuel)?)\s*:\s*(?<balance>$AMOUNT)""", RegexOption.IGNORE_CASE)
        val FEE_MARKER = Regex("""\bfrais\s*:""", RegexOption.IGNORE_CASE)
        val FEE = Regex("""\bfrais\s*:\s*(?<fee>$AMOUNT)""", RegexOption.IGNORE_CASE)
        val REFERENCE = Regex("""\bavec\s+uba\s+(?<reference>[A-Z0-9]{6,})\b""", RegexOption.IGNORE_CASE)
        val INFO = Regex("""^\s*\+infos\s*:\s*(?<info>.+?)\s*$""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    }
}

sealed interface WaveParseResult {
    data class Recognized(val transaction: ParsedWaveTransaction) : WaveParseResult
    data class NotRecognized(val reason: String) : WaveParseResult
}

data class ParsedWaveTransaction(
    val amount: Double,
    val fee: Double?,
    val merchant: String,
    val partyPhone: String?,
    val date: Date,
    val transactionType: TransactionType,
    val balance: Double,
    val providerReference: String?,
    val additionalInfo: String?,
)
