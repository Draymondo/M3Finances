package com.naveenapps.expensemanager.core.data.repository

import com.naveenapps.expensemanager.core.repository.FirebaseSettingsRepository

/**
 * Firebase Remote Config removed along with the rest of Firebase. These URLs were always just
 * remotely-overridable defaults pointing at the original developer's own pages anyway, so this
 * now simply returns those same defaults directly — behaviourally identical to before for
 * anyone who never had a remote override applied.
 */
class FirebaseSettingsRepositoryImpl : FirebaseSettingsRepository {

    override fun getPrivacyURL(): String = DEFAULT_PRIVACY_URL

    override fun getTermsURL(): String = DEFAULT_TERMS_URL

    override fun getAboutUsURL(): String = DEFAULT_ABOUT_US_URL

    override fun getGithubURL(): String = DEFAULT_GITHUB_URL

    override fun getInstagramURL(): String = DEFAULT_INSTAGRAM_URL

    override fun getTwitterURL(): String = DEFAULT_TWITTER_URL

    override fun getFeedbackEmail(): String = DEFAULT_FEEDBACK_EMAIL

    companion object {
        private const val DEFAULT_PRIVACY_URL: String =
            "https://expensemanager.naveenapps.com/privacy-policy"
        private const val DEFAULT_TERMS_URL: String =
            "https://expensemanager.naveenapps.com/terms"
        private const val DEFAULT_ABOUT_US_URL: String = "https://expensemanager.naveenapps.com"
        private const val DEFAULT_GITHUB_URL: String = "https://www.github.com/nkuppan"
        private const val DEFAULT_INSTAGRAM_URL: String =
            "https://www.instagram.com/naveenkumar_kup"
        private const val DEFAULT_TWITTER_URL: String = "https://www.twitter.com/naveenkumarn27"
        private const val DEFAULT_FEEDBACK_EMAIL: String = "naveenkumar@naveenapps.com"
    }
}
