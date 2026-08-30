package com.naveenapps.expensemanager.core.settings.domain.model

/**
 * WITHOUT_ANY_SEPARATOR and WITH_COMMA_SEPARATOR must keep their ordinals (0, 1) — persisted as a
 * raw Int (see `NumberFormatSettingsDatastore`). WITH_COMMA_SEPARATOR is kept only so an already
 * saved preference doesn't break; it's no longer offered as a choice in the settings screen (its
 * grouping/decimal symbols came from the phone's system locale, which made its preview
 * unreliable — see WITH_SPACE_SEPARATOR/WITH_PERIOD_SEPARATOR for the deterministic replacements).
 */
enum class NumberFormatType {
    WITHOUT_ANY_SEPARATOR,
    WITH_COMMA_SEPARATOR,
    WITH_SPACE_SEPARATOR,
    WITH_PERIOD_SEPARATOR,
}


inline fun <reified T> toEnumValue(id: Int): T where T : Enum<T> {
    return enumValues<T>().find { it.ordinal == id }
        ?: enumValues<T>().first()
}
