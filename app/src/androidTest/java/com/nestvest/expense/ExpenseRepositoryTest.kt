package com.nestvest.expense

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.*
import com.nestvest.expense.domain.*
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.PrimaryProfile
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExpenseRepositoryTest {
    @Test fun partialUpdatesValidateVendorAndPreserveOtherFields() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "expense-repository-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            val repo = RoomExpenseRepository(db)
            val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
            val initial = (repo.create(123, profile) as ExpenseWrite.Saved).expense
            val id = initial.expenseId
            assertTrue(repo.edit(id, ExpenseDetail.Vendor("x".repeat(81))) is ExpenseWrite.Failed)
            val unicode = "\uD83D\uDE00".repeat(80)
            assertTrue(repo.edit(id, ExpenseDetail.Vendor(" $unicode ")) is ExpenseWrite.Saved)
            repo.edit(id, ExpenseDetail.Category(KakeiboCategory.Culture))
            val updated = (repo.edit(id, ExpenseDetail.Amount(456)) as ExpenseWrite.Saved).expense
            assertEquals(unicode, updated.vendor)
            assertEquals(KakeiboCategory.Culture, updated.category)
            assertEquals(initial.expenseId, updated.expenseId)
            assertEquals(initial.createdByProfileId, updated.createdByProfileId)
            assertEquals(initial.capturedAtEpochMillis, updated.capturedAtEpochMillis)
            assertNull((repo.edit(id, ExpenseDetail.Vendor(" ")) as ExpenseWrite.Saved).expense.vendor)
            assertEquals(ExpenseWrite.Missing, repo.edit(UUID.randomUUID(), ExpenseDetail.Amount(1)))
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_detail BEFORE UPDATE ON expenses BEGIN SELECT RAISE(ABORT, 'test'); END")
            assertTrue(repo.edit(id, ExpenseDetail.Category(null)) is ExpenseWrite.Failed)
            assertEquals("Culture", db.expenseDao().read(id.toString())!!.category)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun committedChangeSurvivesCleanupFailureAndLaterReconciles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "expense-cleanup-${UUID.randomUUID()}.db"
        val directory = File(context.filesDir, "cleanup-test-${UUID.randomUUID()}").apply { mkdirs() }
        val orphan = File(directory, "orphan.receipt").apply { mkdirs() }
        val child = File(orphan, "block").apply { writeText("block deletion") }
        val db = ProfileDatabase.open(context, name)
        try {
            val repo = RoomExpenseRepository(db, receipts = ReceiptFileStore(context, directory))
            val initial = (repo.create(100, PrimaryProfile(UUID.randomUUID(), "Mira", "USD")) as ExpenseWrite.Saved).expense
            val result = repo.edit(initial.expenseId, ExpenseDetail.Vendor("Saved Place")) as ExpenseWrite.Saved
            assertTrue(result.cleanupPending)
            assertEquals("Saved Place", db.expenseDao().read(initial.expenseId.toString())!!.vendor)
            child.delete()
            assertTrue(repo.reconcileReceipts())
            assertFalse(orphan.exists())
        } finally {
            child.delete(); orphan.delete(); directory.delete()
            db.close(); context.deleteDatabase(name)
        }
    }

    @Test fun receiptReplacementFailureKeepsPreviousImageAndDeleteRemovesOwnedFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "receipt-repository-${UUID.randomUUID()}.db"
        val directory = File(context.filesDir, "images-test-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(context.cacheDir, "source-${UUID.randomUUID()}.png")
        source.outputStream().use {
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val db = ProfileDatabase.open(context, name)
        try {
            val repo = RoomExpenseRepository(db, receipts = ReceiptFileStore(context, directory))
            val expense = (repo.create(100, PrimaryProfile(UUID.randomUUID(), "Mira", "USD")) as ExpenseWrite.Saved).expense
            val saved = repo.attach(expense.expenseId, Uri.fromFile(source)) as ExpenseWrite.Saved
            val original = saved.expense.receiptFileName!!
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_receipt BEFORE UPDATE ON expenses BEGIN SELECT RAISE(ABORT, 'test'); END")
            assertTrue(repo.attach(expense.expenseId, Uri.fromFile(source)) is ExpenseWrite.Failed)
            assertEquals(original, db.expenseDao().read(expense.expenseId.toString())!!.receiptFileName)
            assertEquals(listOf(original), directory.listFiles()!!.map { it.name })
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_receipt")
            val replacement = repo.attach(expense.expenseId, Uri.fromFile(source)) as ExpenseWrite.Saved
            assertNotEquals(original, replacement.expense.receiptFileName)
            assertFalse(File(directory, original).exists())
            assertTrue(repo.edit(expense.expenseId, ExpenseDetail.Receipt(null)) is ExpenseWrite.Saved)
            assertTrue(directory.listFiles()!!.isEmpty())
            assertTrue(repo.attach(expense.expenseId, Uri.fromFile(source)) is ExpenseWrite.Saved)
            assertTrue(repo.delete(expense.expenseId) is ExpenseWrite.Deleted)
            assertTrue(directory.listFiles()!!.isEmpty())
        } finally {
            source.delete()
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
            db.close(); context.deleteDatabase(name)
        }
    }
}
