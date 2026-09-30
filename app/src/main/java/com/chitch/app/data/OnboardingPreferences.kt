package com.chitch.app.data

import android.content.Context

/**
 * Plain SharedPreferences rather than the Room `user` table: [UserEntity][com.chitch.app.data.entity.UserEntity]
 * isn't guaranteed to exist until Settings is first saved, but onboarding needs to know on the
 * very first launch, before any Room row is created.
 */
class OnboardingPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_SEEN_ONBOARDING, false)

    fun setSeenOnboarding() {
        prefs.edit().putBoolean(KEY_SEEN_ONBOARDING, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "chitch_prefs"
        const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
    }
}
