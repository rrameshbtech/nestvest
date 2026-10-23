package com.nestvest.profile.ui

sealed interface ProfileIntent {
    data class NameChanged(val value: String) : ProfileIntent
    data class CurrencyChanged(val code: String) : ProfileIntent
    data object Submit : ProfileIntent
    data object RetryLoad : ProfileIntent
    data object OpenAppearance : ProfileIntent
    data class AvatarChanged(val id: String?) : ProfileIntent
    data class ColorChanged(val id: String?) : ProfileIntent
    data object ApplyAppearance : ProfileIntent
    data object SkipAppearance : ProfileIntent
    data object DismissAppearance : ProfileIntent
}
