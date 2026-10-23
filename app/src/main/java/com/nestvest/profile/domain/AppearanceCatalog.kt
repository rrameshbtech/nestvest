package com.nestvest.profile.domain

object AppearanceCatalog {
    val avatars = setOf("leaf", "home", "sun")
    val colors = setOf("sage", "sand", "sky")

    fun avatar(id: String?): String? = id?.takeIf { it in avatars }
    fun color(id: String?): String? = id?.takeIf { it in colors }
}
