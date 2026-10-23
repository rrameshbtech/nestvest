package com.nestvest.profile.domain

enum class StorageError { FULL, UNAVAILABLE, INVALID_RECORD }

sealed interface ProfileRead {
    data object Absent : ProfileRead
    data class Found(val profile: PrimaryProfile) : ProfileRead
    data class Failed(val error: StorageError) : ProfileRead
}

sealed interface ProfileWrite {
    data class Created(val profile: PrimaryProfile) : ProfileWrite
    data class Existing(val profile: PrimaryProfile) : ProfileWrite
    data class Failed(val error: StorageError) : ProfileWrite
    data object InvalidDraft : ProfileWrite
}

interface ProfileRepository {
    suspend fun read(): ProfileRead
    suspend fun create(draft: ProfileDraft): ProfileWrite
}
