package com.nestvest.expense.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReceiptFileStore(
    private val context: Context,
    private val directory: File = File(context.filesDir, "receipts"),
) {
    suspend fun copy(uri: Uri): String = withContext(Dispatchers.IO) {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Receipt directory unavailable")
        val name = "${UUID.randomUUID()}.receipt"
        val file = File(directory, name)
        try {
            val source = context.contentResolver.openInputStream(uri) ?: throw IOException("Image unavailable")
            source.use { input -> file.outputStream().use { output -> input.copyTo(output); output.fd.sync() } }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Invalid image")
            name
        } catch (error: IOException) {
            discard(file)
            throw error
        } catch (error: SecurityException) {
            discard(file)
            throw IOException("Image access denied", error)
        }
    }

    suspend fun reconcile(referenced: Set<String>): Boolean = withContext(Dispatchers.IO) {
        if (!directory.exists()) return@withContext true
        val files = directory.listFiles() ?: return@withContext false.also {
            Log.w("NestVestStorage", "Receipt directory could not be read")
        }
        files.filter { it.name !in referenced }.fold(true) { success, file -> discard(file) && success }
    }

    private fun discard(file: File): Boolean {
        val removed = !file.exists() || file.delete()
        if (!removed) Log.w("NestVestStorage", "Receipt cleanup pending")
        return removed
    }
}
