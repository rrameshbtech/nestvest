package com.nestvest.dashboard.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardPreferenceRead
import com.nestvest.dashboard.domain.DashboardPreferenceRepository
import com.nestvest.dashboard.domain.DashboardPreferenceWrite
import com.nestvest.dashboard.domain.DashboardOptionCatalog
import com.nestvest.dashboard.domain.pinnedOptionIds
import com.nestvest.dashboard.domain.serializePinnedOptionIds
import com.nestvest.profile.domain.StorageError
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class RoomDashboardPreferenceRepository(
    private val dao: DashboardPreferenceDao,
    private val availableOptionIds: Set<String> = DashboardOptionCatalog.ids,
) : DashboardPreferenceRepository {
    override fun observe(): Flow<DashboardPreferenceRead> = flow {
        dao.initialize()
        emitAll(
            dao.observe().map { entity ->
                if (entity == null) {
                    DashboardPreferenceRead.Failed(StorageError.INVALID_RECORD)
                } else {
                    DashboardPreferenceRead.Found(
                        DashboardPreference(entity.pinnedOptionIds.pinnedOptionIds())
                            .availableTo(availableOptionIds),
                    )
                }
            },
        )
    }.catch { error ->
        when (error) {
            is CancellationException -> throw error
            is SQLiteException, is IOException -> emit(DashboardPreferenceRead.Failed(storageError(error)))
            is IllegalStateException, is IllegalArgumentException ->
                emit(DashboardPreferenceRead.Failed(StorageError.INVALID_RECORD))
            else -> throw error
        }
    }

    override suspend fun save(preference: DashboardPreference): DashboardPreferenceWrite = try {
        dao.save(
            DashboardPreferenceEntity(
                pinnedOptionIds = preference.pinnedOptionIds
                    .intersect(availableOptionIds)
                    .serializePinnedOptionIds(),
            ),
        )
        DashboardPreferenceWrite.Saved
    } catch (error: SQLiteException) {
        diagnose("save", error)
        DashboardPreferenceWrite.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("save", error)
        DashboardPreferenceWrite.Failed(StorageError.UNAVAILABLE)
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalArgumentException) {
        diagnose("save", error)
        DashboardPreferenceWrite.Failed(StorageError.INVALID_RECORD)
    } catch (error: IllegalStateException) {
        diagnose("save", error)
        DashboardPreferenceWrite.Failed(StorageError.INVALID_RECORD)
    }

    private fun storageError(error: Throwable): StorageError =
        if (error is SQLiteFullException) StorageError.FULL else StorageError.UNAVAILABLE

    private fun diagnose(operation: String, error: Exception) {
        Log.w("NestVestStorage", "Dashboard preference $operation failed (${error.javaClass.simpleName})")
    }
}
