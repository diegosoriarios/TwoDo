package com.diego.twodo.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dismissedFlow = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DISMISSED, false))

    val isOnboardingDismissed: StateFlow<Boolean> = dismissedFlow.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_ONBOARDING_DISMISSED) {
                dismissedFlow.value = sharedPreferences.getBoolean(KEY_ONBOARDING_DISMISSED, false)
            }
        }
    }

    suspend fun setOnboardingDismissed(dismissed: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DISMISSED, dismissed).apply()
    }

    private companion object {
        const val PREFS_NAME = "twodo_settings"
        const val KEY_ONBOARDING_DISMISSED = "onboarding_dismissed"
    }
}
