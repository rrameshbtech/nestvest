package com.nestvest.profile.domain

import java.util.UUID

data class PrimaryProfile(
    val profileId: UUID,
    val name: String,
    val preferredCurrency: String,
    val avatarId: String? = null,
    val colorId: String? = null,
)
