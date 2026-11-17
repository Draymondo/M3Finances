package com.naveenapps.expensemanager.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Renders an amount like "164.895\u00A0FCFA" as a large, bold number with the
 * currency unit smaller and muted next to it — the modern fintech look.
 *
 * The split happens at the last separator (space / nbsp / narrow nbsp) and is
 * only applied when the tail contains no digit (i.e. it is a currency unit,
 * not decimals). If no sensible split is found the whole string is rendered
 * unsplit so nothing is ever lost.
 *
 * Digits use tabular figures (tnum) so amounts align vertically in lists.
 */
@Composable
fun AmountText(
    text: String,
    numberStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    unitColor: Color = color.copy(alpha = 0.55f),
    unitSizeFactor: Float = 0.42f,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {
    val separatorIndex = text.indexOfLast {
        it == ' ' || it == '\u00A0' || it == '\u202F'
    }

    val split: Pair<String, String?> = if (separatorIndex > 0 && separatorIndex < text.lastIndex) {
        val head = text.substring(0, separatorIndex).trimEnd()
        val tail = text.substring(separatorIndex + 1).trim()
        if (tail.isNotEmpty() && tail.none { it.isDigit() }) {
            head to tail
        } else {
            text to null
        }
    } else {
        text to null
    }
    val (number, unit) = split

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = horizontalArrangement,
    ) {
        Text(
            text = number,
            style = numberStyle.copy(
                color = color,
                fontFeatureSettings = "tnum",
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
        if (unit != null) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = unit,
                style = numberStyle.copy(
                    color = unitColor,
                    fontSize = numberStyle.fontSize * unitSizeFactor,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
