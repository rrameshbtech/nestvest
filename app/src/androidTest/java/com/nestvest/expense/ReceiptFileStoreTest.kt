package com.nestvest.expense

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.ReceiptFileStore
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReceiptFileStoreTest {
    @Test fun copiesImagePrivatelyAndReconcilesOnlyUnreferencedFiles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "receipt-test-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(context.cacheDir, "image-${UUID.randomUUID()}.png")
        try {
            source.outputStream().use { Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, it) }
            val store = ReceiptFileStore(context, directory)
            val filename = store.copy(Uri.fromFile(source))
            assertTrue(File(directory, filename).exists())
            assertTrue(store.reconcile(setOf(filename)))
            assertTrue(File(directory, filename).exists())
            assertTrue(store.reconcile(emptySet()))
            assertFalse(File(directory, filename).exists())
        } finally {
            source.delete()
            directory.listFiles()?.forEach { it.delete() }
            directory.delete()
        }
    }
    @Test fun rejectsNonImageWithoutKeepingStagedFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "receipt-test-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(context.cacheDir, "invalid-${UUID.randomUUID()}.txt").apply { writeText("not an image") }
        try {
            val store = ReceiptFileStore(context, directory)
            try { store.copy(Uri.fromFile(source)); fail("Expected invalid image failure") }
            catch (_: IOException) { }
            assertTrue(directory.listFiles()!!.isEmpty())
        } finally { source.delete(); directory.delete() }
    }
    @Test fun inaccessibleSourceDoesNotLeaveAStagedImage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "receipt-missing-${UUID.randomUUID()}")
        val missing = File(context.cacheDir, "missing-${UUID.randomUUID()}.png")
        try {
            try {
                ReceiptFileStore(context, directory).copy(Uri.fromFile(missing))
                fail("Expected inaccessible image failure")
            } catch (_: IOException) { }
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally { directory.delete() }
    }
}
