package com.naveenapps.expensemanager.core.model

enum class TransactionSource {
    WAVE,
    ORANGE_MONEY,
    MTN_MOMO,
    MOOV_MONEY,
    DJAMO,
    PAYPAL,
    SMS,
    UNKNOWN,
    SCHEDULED
}

fun TransactionSource.isKnownProvider(): Boolean {
    return this != TransactionSource.UNKNOWN &&
        this != TransactionSource.SMS &&
        this != TransactionSource.SCHEDULED
}