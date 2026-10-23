package com.nestvest.expense

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.ExpenseEntity
import com.nestvest.profile.data.ProfileDatabase
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExpenseEnrichmentPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun independentDetailColumnUpdatesRetainEachOtherAndSurviveReopen() = runBlocking {
        val name = "expense-enrichment-${UUID.randomUUID()}.db"
        val expenseId = "e94dfd2a-23f9-463f-ad55-a4c10b3dfed0"
        val creatorId = "446dc509-0f45-4a92-a4d5-201e9b6f8592"
        var db = ProfileDatabase.open(context, name)
        try {
            db.expenseDao().insert(
                ExpenseEntity(
                    expenseId = expenseId,
                    amountMinorUnits = 1250,
                    currencyCode = "INR",
                    capturedAtEpochMillis = 1_792_677_296_000,
                    createdByProfileId = creatorId,
                ),
            )

            listOf(
                async(Dispatchers.IO) {
                    db.openHelper.writableDatabase.execSQL(
                        "UPDATE expenses SET vendor = ? WHERE expenseId = ?",
                        arrayOf("Corner Shop", expenseId),
                    )
                },
                async(Dispatchers.IO) {
                    db.openHelper.writableDatabase.execSQL(
                        "UPDATE expenses SET category = ? WHERE expenseId = ?",
                        arrayOf("Needs", expenseId),
                    )
                },
                async(Dispatchers.IO) {
                    db.openHelper.writableDatabase.execSQL(
                        "UPDATE expenses SET receiptFileName = ? WHERE expenseId = ?",
                        arrayOf("a1b2c3.receipt", expenseId),
                    )
                },
            ).awaitAll()

            val updated = db.expenseDao().read(expenseId)
            assertNotNull(updated)
            assertEquals("Corner Shop", updated?.vendor)
            assertEquals("Needs", updated?.category)
            assertEquals("a1b2c3.receipt", updated?.receiptFileName)
            assertEquals(1250L, updated?.amountMinorUnits)
            assertEquals(creatorId, updated?.createdByProfileId)

            db.close()
            db = ProfileDatabase.open(context, name)
            val reopened = db.expenseDao().read(expenseId)
            assertNotNull(reopened)
            assertEquals("Corner Shop", reopened?.vendor)
            assertEquals("Needs", reopened?.category)
            assertEquals("a1b2c3.receipt", reopened?.receiptFileName)
            assertEquals(1250L, reopened?.amountMinorUnits)
            assertEquals("INR", reopened?.currencyCode)
            assertEquals(1_792_677_296_000, reopened?.capturedAtEpochMillis)
            assertEquals(creatorId, reopened?.createdByProfileId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun amountCorrectionDoesNotReplaceExpenseIdentityOrCaptureMetadata() = runBlocking {
        val name = "expense-correction-${UUID.randomUUID()}.db"
        val expenseId = UUID.randomUUID().toString()
        val creatorId = UUID.randomUUID().toString()
        val capturedAt = 1_792_677_296_000L
        val db = ProfileDatabase.open(context, name)
        try {
            db.expenseDao().insert(
                ExpenseEntity(
                    expenseId = expenseId,
                    amountMinorUnits = 1250,
                    currencyCode = "INR",
                    capturedAtEpochMillis = capturedAt,
                    createdByProfileId = creatorId,
                    vendor = "Corner Shop",
                    category = "Wants",
                ),
            )

            db.openHelper.writableDatabase.execSQL(
                "UPDATE expenses SET amountMinorUnits = ? WHERE expenseId = ?",
                arrayOf<Any?>(1500L, expenseId),
            )

            val corrected = db.expenseDao().read(expenseId)!!
            assertEquals(1500L, corrected.amountMinorUnits)
            assertEquals(expenseId, corrected.expenseId)
            assertEquals(capturedAt, corrected.capturedAtEpochMillis)
            assertEquals(creatorId, corrected.createdByProfileId)
            assertEquals("Corner Shop", corrected.vendor)
            assertEquals("Wants", corrected.category)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
