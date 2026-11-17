package com.naveenapps.expensemanager.core.designsystem.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Matches the numeric core of a formatted amount, whatever the currency placement:
 * `11 944 FCFA`, `FCFA 11 944`, `$100.00`, `−1 000 F`, `0FCFA`…
 */
private val NUMBER_TOKEN = Regex("[-+−]?\\d[\\d\\u00A0\\u202F .,]*")

private const val THIN_SPACE = "\u202F"

/**
 * Renders a formatted amount with:
 * - **tabular figures** (`tnum`) so digits stop "dancing" when the value changes,
 * - the **currency symbol in a smaller size** (default 62 % of the amount),
 * - a **thin non-breaking space** between the number and its currency.
 *
 * Falls back to plain text (with tabular figures) when no number can be detected.
 *
 * @param amount already formatted amount, currency included (`expenseFlowState.expense`)
 * @param style style of the numeric part — the currency is scaled from its `fontSize`
 * @param color applies to the number; the currency is drawn with the same colour at 72 % alpha
 * @param currencyScale relative size of the currency symbol
 */
@Composable
fun AmountText(
    amount: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    currencyScale: Float = 0.62f,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    val annotated = remember(amount, style, color, currencyScale) {
        buildAmountText(amount, style, color, currencyScale)
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

private fun buildAmountText(
    raw: String,
    style: TextStyle,
    color: Color,
    currencyScale: Float,
): AnnotatedString {
    val match = NUMBER_TOKEN.find(raw) ?: return AnnotatedString(raw)

    val prefix = raw.substring(0, match.range.first).trim()
    val number = match.value.trimEnd(' ', '.', ',', '\u00A0', '\u202F')
    if (number.isEmpty()) return AnnotatedString(raw)

    val suffix = raw.substring(match.range.first + number.length).trim()

    // Devise en préfixe : le signe éventuel (« -€1 200 ») appartient au nombre,
    // pas à la devise — sinon il hériterait de la taille réduite.
    val sign = prefix.filter { it == '-' || it == '+' || it == '−' }
    val prefixCurrency = prefix.replace("-", "").replace("+", "").replace("−", "").trim()

    if (prefixCurrency.isEmpty() && suffix.isEmpty()) {
        return AnnotatedString("$sign$number")
    }

    val baseSize = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 16.sp
    val currencySize = baseSize * currencyScale
    val currencyColor = if (color == Color.Unspecified) Color.Unspecified else color.copy(alpha = 0.72f)
    val currencyStyle = SpanStyle(fontSize = currencySize, color = currencyColor)

    return buildAnnotatedString {
        if (sign.isNotEmpty()) {
            append(sign)
        }
        if (prefixCurrency.isNotEmpty()) {
            withStyle(currencyStyle) { append(prefixCurrency) }
            append(THIN_SPACE)
        }
        append(number)
        if (suffix.isNotEmpty()) {
            append(THIN_SPACE)
            withStyle(currencyStyle) { append(suffix) }
        }
    }
}
