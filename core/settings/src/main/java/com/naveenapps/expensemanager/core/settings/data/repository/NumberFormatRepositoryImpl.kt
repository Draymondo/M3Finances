package com.naveenapps.expensemanager.core.settings.data.repository

import com.naveenapps.expensemanager.core.settings.domain.model.NumberFormatType
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatSettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NumberFormatRepositoryImpl(
    private val locale: Locale = Locale.getDefault(),
    numberFormatSettingRepository: NumberFormatSettingRepository,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : NumberFormatRepository {

    private var numberFormatType: NumberFormatType = NumberFormatType.WITHOUT_ANY_SEPARATOR

    init {
        numberFormatSettingRepository.getNumberFormatType().onEach {
            numberFormatType = it
        }.launchIn(coroutineScope)
    }

    private fun getFormatter(): NumberFormat {
        return when (numberFormatType) {
            NumberFormatType.WITHOUT_ANY_SEPARATOR -> {
                ApplicationNumberFormatter.getNumberFormatWithoutGrouping(locale)
            }

            NumberFormatType.WITH_COMMA_SEPARATOR -> {
                ApplicationNumberFormatter.getNumberFormatWithGrouping(locale)
            }

            NumberFormatType.WITH_SPACE_SEPARATOR -> {
                ApplicationNumberFormatter.getNumberFormatWithSpaceSeparator()
            }

            NumberFormatType.WITH_PERIOD_SEPARATOR -> {
                ApplicationNumberFormatter.getNumberFormatWithPeriodSeparator()
            }
        }
    }

    /**
     * Converts a Double or numeric string into localized display format
     * Example: 1234.5 → "1.234,5" (DE) or "1234.5" (US, no grouping)
     */
    override fun formatForDisplay(value: Double): String {
        return getFormatter().format(value)
    }

    /**
     * Converts localized string into a plain string with '.' as decimal
     * Example: "1.234,5" (DE) → "1234.5"
     */
    override fun formatForEditing(localizedValue: String): String {
        val parsed = parseToDouble(localizedValue)
        return parsed?.toEditableString() ?: localizedValue
    }

    /**
     * Converts localized string into a plain string with '.' as decimal
     * Example: "1.234,5" (DE) → "1234.5"
     */
    override fun formatForEditing(value: Double): String {
        return value.toEditableString()
    }

    /** Kotlin's `Double.toString()` always appends a decimal part even for whole numbers
     * ("50000.0", "0.0"), which looks like a typo/bug to someone editing an amount that's
     * naturally a whole number (most FCFA amounts never use decimals at all). Editing a field
     * should show the cleanest re-typeable form: "50000" rather than "50000.0", while genuine
     * decimals ("1234.5") are left untouched since `toString()` doesn't over-pad those. */
    private fun Double.toEditableString(): String {
        val raw = this.toString()
        return if (raw.endsWith(".0")) raw.dropLast(2) else raw
    }

    override fun parseToDouble(localizedValue: String): Double? {
        if (localizedValue.isBlank()) return null
        
        try {
            // Robust parsing: strip spaces and standardise decimal separator
            var clean = localizedValue.replace(" ", "").replace("\u00A0", "")
            val lastComma = clean.lastIndexOf(',')
            val lastDot = clean.lastIndexOf('.')
            
            if (lastComma > lastDot) {
                // e.g. "1.234,50" -> "1234.50"
                clean = clean.replace(".", "").replace(",", ".")
            } else if (lastDot > lastComma) {
                // e.g. "1,234.50" -> "1234.50"
                clean = clean.replace(",", "")
            } else {
                // Only one type of separator or none, e.g. "12,50" or "12.50"
                clean = clean.replace(",", ".")
            }
            
            val value = clean.toDoubleOrNull()
            if (value != null) return value
        } catch (e: Exception) {
            // Ignored
        }

        // Fallback to strict formatter
        val formatter = getFormatter()
        return try {
            formatter.parse(localizedValue)?.toDouble()
        } catch (e: ParseException) {
            null
        }
    }
}


object ApplicationNumberFormatter {

    private val normalNumberFormatCache = ConcurrentHashMap<Locale, NumberFormat>()
    private val defaultNumberFormatCache = ConcurrentHashMap<Locale, NumberFormat>()

    fun getNumberFormatWithGrouping(locale: Locale): NumberFormat {
        return normalNumberFormatCache.computeIfAbsent(locale) {
            NumberFormat.getNumberInstance(locale).apply {
                isGroupingUsed = true
                maximumFractionDigits = 1
                minimumFractionDigits = 0
            }
        }
    }

    fun getNumberFormatWithoutGrouping(locale: Locale): NumberFormat {
        return defaultNumberFormatCache.computeIfAbsent(locale) {
            NumberFormat.getNumberInstance(locale).apply {
                isGroupingUsed = false
                maximumFractionDigits = 1
                minimumFractionDigits = 1
            }
        }
    }

    private val spaceSeparatorFormat: NumberFormat by lazy { buildDeterministicFormat(groupingSeparator = ' ') }
    private val periodSeparatorFormat: NumberFormat by lazy { buildDeterministicFormat(groupingSeparator = '.') }

    /**
     * Same grouping/decimal-digit shape as [getNumberFormatWithGrouping], but with the grouping
     * and decimal symbols fixed explicitly rather than taken from `Locale.getDefault()` — so the
     * result is the same regardless of the phone's system locale/region, and matches what the
     * settings screen's preview text actually shows. Decimal separator is a comma, matching the
     * comma already used elsewhere in the app (e.g. [getNumberFormatWithoutGrouping] under a
     * French-like locale).
     */
    private fun buildDeterministicFormat(groupingSeparator: Char): NumberFormat {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            this.groupingSeparator = groupingSeparator
            decimalSeparator = ','
        }
        return (DecimalFormat("#,##0.#", symbols)).apply {
            isGroupingUsed = true
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }

    fun getNumberFormatWithSpaceSeparator(): NumberFormat = spaceSeparatorFormat

    fun getNumberFormatWithPeriodSeparator(): NumberFormat = periodSeparatorFormat
}
