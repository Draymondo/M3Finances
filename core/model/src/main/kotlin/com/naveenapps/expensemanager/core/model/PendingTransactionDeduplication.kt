package com.naveenapps.expensemanager.core.model

import java.security.MessageDigest
import java.util.Locale

/**
 * Utility for computing deterministic financial identities and deduplication keys
 * for pending transactions.
 */
object PendingTransactionDeduplication {

    /**
     * Cross-channel duplicate detection window in milliseconds (90 seconds).
     */
    const val CROSS_CHANNEL_WINDOW_MS = 90 * 1000L

    /**
     * Sliding window duration in milliseconds (5 minutes).
     * Two transactions from the same source with the same financial identity within this
     * window are considered duplicate events.
     */
    const val SLIDING_WINDOW_MS = 5 * 60 * 1000L

    /**
     * Checks whether two timestamps fall within the sliding window distance (strictly less than windowMs).
     */
    fun isWithinSlidingWindow(
        firstTimestamp: Long,
        secondTimestamp: Long,
        windowMs: Long = SLIDING_WINDOW_MS,
    ): Boolean {
        if (windowMs <= 0) return firstTimestamp == secondTimestamp
        return kotlin.math.abs(firstTimestamp - secondTimestamp) < windowMs
    }

    /**
     * Canonical SHA-256 hash (`source|type|amount|senderOrReceiver|fee`)
     * representing the stable financial identity.
     */
    fun computeDeduplicationKey(
        source: TransactionSource,
        type: TransactionType,
        amount: Double,
        senderOrReceiver: String?,
        fee: Double?,
    ): String {
        val canonicalAmount = String.format(Locale.US, "%.2f", amount)
        val canonicalFee = fee?.let { String.format(Locale.US, "%.2f", it) } ?: "none"
        val canonicalParty = senderOrReceiver?.trim()?.lowercase(Locale.ROOT) ?: "none"
        val payload = "${source.name}|${type.name}|$canonicalAmount|$canonicalParty|$canonicalFee"

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte ->
            val value = byte.toInt() and 0xff
            "${HEX_DIGITS[value ushr 4]}${HEX_DIGITS[value and 0x0f]}"
        }
    }

    private val HEX_DIGITS = "0123456789abcdef"
}
