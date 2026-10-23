package com.nestvest.expense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert suspend fun insert(expense: ExpenseEntity)
    @Query("SELECT * FROM expenses WHERE expenseId = :id")
    suspend fun read(id: String): ExpenseEntity?
    @Query("SELECT * FROM expenses ORDER BY capturedAtEpochMillis DESC, expenseId DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses WHERE capturedAtEpochMillis >= :start AND capturedAtEpochMillis < :end ORDER BY capturedAtEpochMillis DESC, expenseId DESC")
    fun observe(start: Long, end: Long): Flow<List<ExpenseEntity>>
    @Query("UPDATE expenses SET vendor = :value WHERE expenseId = :id")
    suspend fun vendor(id: String, value: String?): Int
    @Query("UPDATE expenses SET category = :value WHERE expenseId = :id")
    suspend fun category(id: String, value: String?): Int
    @Query("UPDATE expenses SET amountMinorUnits = :value WHERE expenseId = :id")
    suspend fun amount(id: String, value: Long): Int
    @Query("UPDATE expenses SET receiptFileName = :value WHERE expenseId = :id")
    suspend fun receipt(id: String, value: String?): Int
    @Query("DELETE FROM expenses WHERE expenseId = :id")
    suspend fun delete(id: String): Int
    @Query("SELECT receiptFileName FROM expenses WHERE receiptFileName IS NOT NULL")
    suspend fun receiptFiles(): List<String>
}
