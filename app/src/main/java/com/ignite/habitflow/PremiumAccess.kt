package com.ignite.habitflow

import android.content.Context

/** Central premium entitlement state. Billing will update this value when purchases are added. */
object PremiumAccess {
    private const val PREFS = "habitflow"
    private const val KEY_PREMIUM = "premium_active"

    fun isPremium(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM, false)

    fun setPremiumForBilling(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREMIUM, active)
            .apply()
    }
}

enum class PremiumFeature(val title: String) {
    UNLIMITED_HABITS("Unlimited habits and categories"),
    ADVANCED_INSIGHTS("Advanced weekly, monthly and yearly insights"),
    CUSTOM_FOCUS("Custom focus and break presets"),
    FOCUS_TRENDS("Focus history and deeper trends"),
    THEMES("Themes and home customization"),
    RECURRING_SUBTASKS("Subtasks and recurring task controls")
}
