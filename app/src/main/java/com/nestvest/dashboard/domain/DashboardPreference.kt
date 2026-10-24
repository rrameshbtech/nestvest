package com.nestvest.dashboard.domain

import com.nestvest.profile.domain.StorageError
import kotlinx.coroutines.flow.Flow

data class DashboardPreference(val pinnedOptionIds: Set<String>) {
    companion object {
        val DEFAULT = DashboardPreference(setOf("expense_capture", "expense_history"))
    }

    fun availableTo(catalogIds: Set<String>) = copy(pinnedOptionIds = pinnedOptionIds.intersect(catalogIds))
}

sealed interface DashboardPreferenceRead {
    data class Found(val preference: DashboardPreference) : DashboardPreferenceRead
    data class Failed(val error: StorageError) : DashboardPreferenceRead
}

sealed interface DashboardPreferenceWrite {
    data object Saved : DashboardPreferenceWrite
    data class Failed(val error: StorageError) : DashboardPreferenceWrite
}

interface DashboardPreferenceRepository {
    fun observe(): Flow<DashboardPreferenceRead>
    suspend fun save(preference: DashboardPreference): DashboardPreferenceWrite
}

internal fun String.pinnedOptionIds(): Set<String> = split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSet()

internal fun Set<String>.serializePinnedOptionIds(): String = sorted().joinToString(",")
