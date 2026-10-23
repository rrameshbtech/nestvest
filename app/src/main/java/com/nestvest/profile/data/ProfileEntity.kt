package com.nestvest.profile.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.CurrencyCatalog
import java.util.UUID

@Entity(tableName = "primary_profile", indices = [Index(value = ["primarySlot"], unique = true)])
data class ProfileEntity(
    @PrimaryKey val profileId: String,
    val name: String,
    val preferredCurrency: String,
    val avatarId: String?,
    val colorId: String?,
    val primarySlot: Int = 1,
) {
    fun toProfile(): PrimaryProfile {
        require(name.isNotBlank())
        require(preferredCurrency in CurrencyCatalog.codes)
        return PrimaryProfile(UUID.fromString(profileId), name, preferredCurrency, avatarId, colorId)
    }
}
