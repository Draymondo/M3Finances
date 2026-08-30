package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.repository.AnalyticsRepository

/**
 * No-op implementation. This is a personal-use fork with tracking removed entirely
 * (no Firebase Analytics, no other analytics SDK) — nothing is collected or sent anywhere.
 * Kept as a real implementation (rather than deleting the interface) so call sites elsewhere
 * in the app don't need to change.
 */
class AnalyticsRepositoryImpl : AnalyticsRepository {

    override fun trackAppOpenEvent() = Unit

    override fun logEvent(eventName: String, params: Map<String, String>) = Unit

    override fun setCurrentScreen(screenName: String) = Unit

    override fun setUserProperties() = Unit
}
