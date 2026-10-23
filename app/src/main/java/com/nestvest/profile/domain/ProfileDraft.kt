package com.nestvest.profile.domain

data class ProfileDraft(
    val name: String = "",
    val currencyCode: String = "",
    val avatarId: String? = null,
    val colorId: String? = null,
) {
    val validName: Boolean get() = name.isNotBlank()
    val validCurrency: Boolean get() = currencyCode in CurrencyCatalog.codes
}
