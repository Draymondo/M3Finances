package com.naveenapps.expensemanager.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.naveenapps.designsystem.theme.NaveenAppsTheme

@Composable
fun DynamicAppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    NaveenAppsTheme(isDarkTheme = isDarkTheme) {
        val context = LocalContext.current
        val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        
        val colorScheme = when {
            isDynamicColorSupported && isDarkTheme -> dynamicDarkColorScheme(context)
            isDynamicColorSupported && !isDarkTheme -> dynamicLightColorScheme(context)
            else -> MaterialTheme.colorScheme
        }

        // Surcouche MaterialTheme pour forcer l'utilisation des couleurs dynamiques (Material You)
        // tout en gardant la typographie et les formes définies par NaveenAppsTheme.
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}

