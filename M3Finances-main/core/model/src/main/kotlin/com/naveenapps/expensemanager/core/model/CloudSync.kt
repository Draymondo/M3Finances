package com.naveenapps.expensemanager.core.model

enum class CloudSyncResolution {
    KEEP_LOCAL,
    USE_CLOUD,
}

enum class CloudSyncOutcome {
    Pushed,
    Restored,
    NoOp,
    Conflict,
}
