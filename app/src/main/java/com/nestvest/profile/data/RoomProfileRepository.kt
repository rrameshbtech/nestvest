package com.nestvest.profile.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import androidx.room.withTransaction
import com.nestvest.profile.domain.*
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException

class RoomProfileRepository(private val database: ProfileDatabase) : ProfileRepository {
    override suspend fun read(): ProfileRead = try {
        database.profileDao().read()?.let { ProfileRead.Found(it.toProfile()) } ?: ProfileRead.Absent
    } catch (error: SQLiteException) {
        diagnose("read", error)
        ProfileRead.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("read", error)
        ProfileRead.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("invalid stored profile", error)
        ProfileRead.Failed(StorageError.INVALID_RECORD)
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalStateException) {
        diagnose("database state", error)
        ProfileRead.Failed(StorageError.INVALID_RECORD)
    }

    override suspend fun create(draft: ProfileDraft): ProfileWrite {
        if (!draft.validName || !draft.validCurrency) {
            Log.w("NestVestStorage", "Rejected invalid profile draft")
            return ProfileWrite.InvalidDraft
        }
        return try {
            database.withTransaction {
                val existing = database.profileDao().read()
                if (existing != null) {
                    ProfileWrite.Existing(existing.toProfile())
                } else {
                    val entity = ProfileEntity(
                        UUID.randomUUID().toString(), draft.name.trim(), draft.currencyCode,
                        AppearanceCatalog.avatar(draft.avatarId), AppearanceCatalog.color(draft.colorId),
                    )
                    if (entity.avatarId != draft.avatarId || entity.colorId != draft.colorId) {
                        Log.w("NestVestStorage", "Unavailable appearance replaced with default")
                    }
                    database.profileDao().insert(entity)
                    ProfileWrite.Created(entity.toProfile())
                }
            }
        } catch (error: SQLiteException) {
            diagnose("create", error)
            ProfileWrite.Failed(storageError(error))
        } catch (error: IOException) {
            diagnose("create", error)
            ProfileWrite.Failed(StorageError.UNAVAILABLE)
        } catch (error: IllegalArgumentException) {
            diagnose("invalid stored profile", error)
            ProfileWrite.Failed(StorageError.INVALID_RECORD)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IllegalStateException) {
            diagnose("database state", error)
            ProfileWrite.Failed(StorageError.INVALID_RECORD)
        }
    }

    private fun storageError(error: SQLiteException): StorageError =
        if (error is SQLiteFullException) StorageError.FULL else StorageError.UNAVAILABLE

    private fun diagnose(operation: String, error: Exception) {
        Log.w("NestVestStorage", "$operation failed (${error.javaClass.simpleName})")
    }
}
