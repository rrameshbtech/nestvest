package com.nestvest.expense.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import com.nestvest.expense.domain.*
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.StorageError
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class RoomReminderRepository(database: ProfileDatabase) : ReminderRepository {
    private val dao = database.reminderPreferenceDao()

    override fun observe() = dao.observe().onStart { dao.initialize() }
        .map<ReminderPreferenceEntity?, ReminderRead> {
            ReminderRead.Found(requireNotNull(it).toPreference())
        }.catch { emit(ReminderRead.Failed(diagnose(it))) }

    override suspend fun read(): ReminderRead = try {
        dao.initialize()
        ReminderRead.Found(requireNotNull(dao.read()).toPreference())
    } catch (error: Exception) { ReminderRead.Failed(diagnose(error)) }

    override suspend fun update(enabled: Boolean, minutes: Int): ReminderWrite = write {
        require(minutes in 0..1439)
        dao.initialize()
        dao.update(enabled, minutes)
    }

    override suspend fun markPermissionPrompted(): ReminderWrite = write {
        dao.initialize()
        dao.markPrompted()
    }

    override suspend fun claim(date: LocalDate): ReminderClaim = try {
        dao.initialize()
        if (dao.claim(date.toString()) == 1) ReminderClaim.Claimed else ReminderClaim.AlreadyClaimed
    } catch (error: Exception) { ReminderClaim.Failed(diagnose(error)) }

    private suspend fun write(block: suspend () -> Unit): ReminderWrite = try {
        block()
        ReminderWrite.Saved
    } catch (error: Exception) { ReminderWrite.Failed(diagnose(error)) }

    private fun diagnose(error: Throwable): StorageError {
        if (error is CancellationException) throw error
        if (error !is SQLiteException && error !is IOException &&
            error !is IllegalArgumentException && error !is IllegalStateException) throw error
        Log.w("NestVestStorage", "Reminder operation failed (${error.javaClass.simpleName})")
        return when (error) {
            is SQLiteFullException -> StorageError.FULL
            is IllegalArgumentException, is IllegalStateException -> StorageError.INVALID_RECORD
            else -> StorageError.UNAVAILABLE
        }
    }
}
