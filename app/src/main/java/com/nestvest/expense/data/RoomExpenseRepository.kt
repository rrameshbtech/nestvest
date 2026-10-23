package com.nestvest.expense.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import android.net.Uri
import androidx.room.withTransaction
import com.nestvest.expense.domain.*
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.StorageError
import java.io.IOException
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomExpenseRepository(
    private val database: ProfileDatabase,
    private val clock: Clock = Clock.systemUTC(),
    private val newId: () -> UUID = UUID::randomUUID,
    private val receipts: ReceiptFileStore? = null,
) : ExpenseRepository, ExpenseEditor {
    private val fileLock = Mutex()
    override fun observe(startInclusive: Long, endExclusive: Long) =
        observeRows(database.expenseDao().observe(startInclusive, endExclusive))

    override fun observeAll() = observeRows(database.expenseDao().observeAll())

    private fun observeRows(rows: Flow<List<ExpenseEntity>>): Flow<ExpenseRead> =
        rows.map<List<ExpenseEntity>, ExpenseRead> { entities ->
            ExpenseRead.Found(entities.map { it.toExpense() })
        }.catch { error ->
            when (error) {
                is CancellationException -> throw error
                is SQLiteException, is IOException, is IllegalArgumentException, is IllegalStateException ->
                    emit(ExpenseRead.Failed(diagnose(error)))
                else -> throw error
            }
        }

    override suspend fun create(amount: Long, profile: PrimaryProfile): ExpenseWrite = write {
        if (amount <= 0) {
            Log.w("NestVestStorage", "Rejected invalid expense amount")
            return@write ExpenseWrite.Invalid
        }
        val entity = ExpenseEntity(newId().toString(), amount, profile.preferredCurrency,
            clock.millis(), profile.profileId.toString())
        val expense = entity.toExpense()
        database.expenseDao().insert(entity)
        ExpenseWrite.Saved(expense)
    }

    override suspend fun edit(id: UUID, detail: ExpenseDetail): ExpenseWrite = fileLock.withLock {
        editLocked(id, detail)
    }

    private suspend fun editLocked(id: UUID, detail: ExpenseDetail): ExpenseWrite = write {
        val dao = database.expenseDao()
        val result = database.withTransaction {
            val old = dao.read(id.toString()) ?: return@withTransaction ExpenseWrite.Missing
            val value = when (detail) {
                is ExpenseDetail.Vendor -> old.copy(vendor = detail.value.trim().ifEmpty { null })
                is ExpenseDetail.Category -> old.copy(category = detail.value?.name)
                is ExpenseDetail.Amount -> old.copy(amountMinorUnits = detail.value)
                is ExpenseDetail.Receipt -> old.copy(receiptFileName = detail.value)
            }
            val domain = value.toExpense()
            val changed = when (detail) {
                is ExpenseDetail.Vendor -> dao.vendor(old.expenseId, value.vendor)
                is ExpenseDetail.Category -> dao.category(old.expenseId, value.category)
                is ExpenseDetail.Amount -> dao.amount(old.expenseId, value.amountMinorUnits)
                is ExpenseDetail.Receipt -> dao.receipt(old.expenseId, value.receiptFileName)
            }
            check(changed == 1) { "Expense update did not affect one record" }
            ExpenseWrite.Saved(domain)
        }
        if (result is ExpenseWrite.Saved)
            result.copy(cleanupPending = !cleanupLocked()) else result
    }

    suspend fun attach(id: UUID, uri: Uri): ExpenseWrite = fileLock.withLock {
        val store = receipts ?: return@withLock ExpenseWrite.Failed(StorageError.UNAVAILABLE)
        val result = write {
            val filename = store.copy(uri)
            editLocked(id, ExpenseDetail.Receipt(filename))
        }
        if (result !is ExpenseWrite.Saved) cleanupLocked()
        result
    }

    override suspend fun delete(id: UUID): ExpenseWrite = fileLock.withLock {
        write {
            if (database.expenseDao().delete(id.toString()) == 0) ExpenseWrite.Missing
            else ExpenseWrite.Deleted(cleanupPending = !cleanupLocked())
        }
    }

    suspend fun reconcileReceipts(): Boolean = fileLock.withLock { cleanupLocked() }

    private suspend fun cleanupLocked(): Boolean {
        val store = receipts ?: return true
        return try {
            store.reconcile(database.expenseDao().receiptFiles().toSet())
        } catch (error: SQLiteException) {
            diagnose(error); false
        } catch (error: IOException) {
            diagnose(error); false
        } catch (error: CancellationException) {
            throw error
        } catch (error: IllegalStateException) {
            diagnose(error); false
        }
    }

    private suspend fun write(block: suspend () -> ExpenseWrite): ExpenseWrite = try {
        block()
    } catch (error: SQLiteException) {
        ExpenseWrite.Failed(diagnose(error))
    } catch (error: IOException) {
        ExpenseWrite.Failed(diagnose(error))
    } catch (error: IllegalArgumentException) {
        ExpenseWrite.Failed(diagnose(error))
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalStateException) {
        ExpenseWrite.Failed(diagnose(error))
    }

    private fun diagnose(error: Throwable): StorageError {
        Log.w("NestVestStorage", "Expense operation failed (${error.javaClass.simpleName})")
        return when (error) {
            is SQLiteFullException -> StorageError.FULL
            is IllegalArgumentException, is IllegalStateException -> StorageError.INVALID_RECORD
            else -> StorageError.UNAVAILABLE
        }
    }
}
