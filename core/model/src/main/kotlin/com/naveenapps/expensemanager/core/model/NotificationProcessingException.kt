package com.naveenapps.expensemanager.core.model

/**
 * Classification d'une erreur métier lors du traitement d'une notification.
 *
 * [UNKNOWN] reste volontairement prudent : le worker peut la retenter dans une limite
 * bornée, sans prétendre qu'il s'agit d'une erreur temporaire Gemini confirmée.
 */
enum class ProcessingErrorType {
    TEMPORARY,
    PERMANENT,
    UNKNOWN,
}

/** Erreur transportée par [Resource.Error] avec son comportement de retry connu. */
class NotificationProcessingException(
    val type: ProcessingErrorType,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
