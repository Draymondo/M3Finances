package com.naveenapps.expensemanager.core.designsystem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.designsystem.R
import java.util.Date

@Composable
fun ClickableTextField(
    label: Int,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val isFocused: Boolean by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onClick.invoke()
            focusManager.clearFocus(force = true)
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onClick.invoke()
            focusManager.clearFocus(force = true)
        }
    }

    val gesture = Modifier.pointerInput(onClick) {
        detectTapGestures(
            onPress = { _ ->
                onClick()
            },
            onTap = { _ ->
                onClick()
            }
        )
    }

    OutlinedTextField(
        modifier = modifier.then(gesture),
        interactionSource = interactionSource,
        value = value,
        singleLine = true,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = "",
                )
            }
        } else {
            null
        },
        label = {
            Text(text = stringResource(id = label))
        },
        onValueChange = {},
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(8.dp)
    )
}

/**
 * Champ « valeur » sans label flottant : label au-dessus, valeur en dessous,
 * icône optionnelle à droite.
 *
 * C'est le remplacement du couple label flottant + icône de tête qui rognait la
 * date (« 16/11/2… ») dès que le champ partageait la largeur avec l'heure. Ici
 * la valeur dispose de toute la largeur du champ et reste sur une seule ligne.
 */
@Composable
fun ClickableValueField(
    label: Int,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(id = label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (leadingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun StringTextField(
    value: String,
    isError: Boolean,
    onValueChange: ((String) -> Unit)?,
    label: Int,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    minLines: Int = 1,
    singleLine: Boolean = minLines == 1,
    errorMessage: String = "",
    enabled: Boolean = true,
) {
    AutoSelectTextField(
        text = value,
        onTextChange = { onValueChange?.invoke(it) },
        label = {
            Text(
                text = stringResource(id = label),
                overflow = TextOverflow.Ellipsis,
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            )
        },
        singleLine = singleLine,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
    )
}

@Composable
fun DecimalTextField(
    value: String,
    isError: Boolean,
    onValueChange: ((String) -> Unit)?,
    label: Int,
    modifier: Modifier = Modifier,
    errorMessage: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    trailingIcon: @Composable (() -> Unit)? = null,
    textStyle: TextStyle? = null,
) {
    AutoSelectTextField(
        text = value,
        onTextChange = { onValueChange?.invoke(it) },
        label = {
            Text(
                text = stringResource(id = label),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        singleLine = true,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
        keyboardOptions = keyboardOptions.copy(
            keyboardType = KeyboardType.Decimal,
        ),
        trailingIcon = trailingIcon,
        textStyle = textStyle,
    )
}

@Composable
fun NumberTextField(
    value: String,
    isError: Boolean,
    onValueChange: ((String) -> Unit)?,
    label: Int,
    modifier: Modifier = Modifier,
    errorMessage: String = "",
    placeholder: String? = null,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    AutoSelectTextField(
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        text = value,
        onTextChange = { onValueChange?.invoke(it) },
        label = {
            Text(
                text = stringResource(id = label),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        placeholder = placeholder,
        supportingText = supportingText,
        singleLine = true,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        )
    )
}

@Preview
@Composable
private fun TextFieldPreviews() {
    NaveenAppsPreviewTheme {
        Column {
            NumberTextField(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                value = "Sample",
                isError = true,
                onValueChange = {},
                label = R.string.choose_color,
                errorMessage = "Should not add the value",
            )
            DecimalTextField(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                value = "1.00",
                isError = true,
                onValueChange = { },
                label = R.string.choose_color,
                errorMessage = "Should not add the value",
            )
            StringTextField(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                value = "Sample",
                isError = true,
                onValueChange = { },
                label = R.string.choose_color,
                errorMessage = "Should not add the value",
            )
            ClickableTextField(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                value = Date().toCompleteDateWithDate(),
                label = R.string.choose_color,
                leadingIcon = Icons.Default.EditCalendar,
                onClick = {},
            )
        }
    }
}
