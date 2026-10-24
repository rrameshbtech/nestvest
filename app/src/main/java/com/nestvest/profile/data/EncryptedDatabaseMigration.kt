package com.nestvest.profile.data

import android.util.Log
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase

internal class EncryptedDatabaseMigration(
    private val databaseFile: File,
    private val passphrase: ByteArray,
) {
    fun migrateIfNeeded() {
        recoverInterruptedMigration()
        if (!databaseFile.exists() || !hasPlaintextHeader(databaseFile)) return

        val databaseVersion = checkpointPlaintextDatabase()
        val backup = backupFile()
        val temporary = temporaryFile()
        if (backup.exists()) {
            error("A database migration backup already exists; recovery is required")
        }

        Files.move(databaseFile.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE)
        try {
            temporary.delete()
            exportPlaintextToEncrypted(backup, temporary, databaseVersion)
            check(isEncryptedAndReadable(temporary)) { "Encrypted database verification failed" }
            Files.move(temporary.toPath(), databaseFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
            check(isEncryptedAndReadable(databaseFile)) { "Encrypted database verification failed after replacement" }
            check(backup.delete()) { "Could not remove verified plaintext migration backup" }
            deleteSidecars()
        } catch (error: Exception) {
            try {
                if (
                    backup.exists() &&
                    (!databaseFile.exists() ||
                        (!hasPlaintextHeader(databaseFile) && !isEncryptedAndReadable(databaseFile)))
                ) {
                    if (databaseFile.exists()) check(databaseFile.delete()) {
                        "Could not remove the incomplete encrypted database file"
                    }
                    Files.move(backup.toPath(), databaseFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                }
            } catch (recoveryError: Exception) {
                error.addSuppressed(recoveryError)
            }
            try {
                check(!temporary.exists() || temporary.delete()) {
                    "Could not remove the incomplete encrypted database file"
                }
            } catch (cleanupError: Exception) {
                error.addSuppressed(cleanupError)
            }
            throw IllegalStateException("Could not encrypt the existing local database", error)
        }
    }

    private fun recoverInterruptedMigration() {
        val backup = backupFile()
        val temporary = temporaryFile()
        if (databaseFile.exists()) {
            if (hasPlaintextHeader(databaseFile)) {
                check(!temporary.exists() || temporary.delete()) {
                    "Could not remove the incomplete encrypted database file"
                }
                check(!backup.exists() || backup.delete()) {
                    "Could not remove the stale plaintext migration backup"
                }
                return
            }
            if (!isEncryptedAndReadable(databaseFile)) {
                check(backup.exists() && hasPlaintextHeader(backup)) {
                    "Encrypted database cannot be opened with its stored key and no recoverable source exists"
                }
                check(databaseFile.delete()) { "Could not remove the incomplete encrypted database file" }
                Files.move(backup.toPath(), databaseFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                check(hasPlaintextHeader(databaseFile)) { "Restored plaintext database failed verification" }
                check(!temporary.exists() || temporary.delete()) {
                    "Could not remove the incomplete encrypted database file"
                }
                return
            }
            check(!temporary.exists() || temporary.delete()) {
                "Could not remove the incomplete encrypted database file"
            }
            check(!backup.exists() || backup.delete()) {
                "Could not remove the plaintext migration backup"
            }
            deleteSidecars()
            return
        }

        if (temporary.exists() && isEncryptedAndReadable(temporary)) {
            Files.move(temporary.toPath(), databaseFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
            check(isEncryptedAndReadable(databaseFile)) { "Recovered encrypted database failed verification" }
            check(!backup.exists() || backup.delete()) {
                "Could not remove the plaintext migration backup"
            }
        } else if (backup.exists()) {
            Files.move(backup.toPath(), databaseFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
            check(!temporary.exists() || temporary.delete()) {
                "Could not remove the incomplete encrypted database file"
            }
        }
    }

    private fun exportPlaintextToEncrypted(source: File, destination: File, databaseVersion: Int) {
        System.loadLibrary("sqlcipher")
        val plaintext = CipherDatabase.openDatabase(
            source.absolutePath,
            ByteArray(0),
            null,
            // ATTACH inherits creation permission from the main connection.
            CipherDatabase.OPEN_READWRITE or CipherDatabase.CREATE_IF_NECESSARY,
            { _, error -> throw error },
            null,
        )
        try {
            val destinationPath = destination.absolutePath.replace("'", "''")
            val passphraseText = String(passphrase, StandardCharsets.US_ASCII)
            plaintext.execSQL(
                "ATTACH DATABASE '$destinationPath' AS encrypted KEY '$passphraseText'",
            )
            try {
                plaintext.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { cursor ->
                    check(cursor.moveToFirst()) { "SQLCipher did not export the existing database" }
                }
            } finally {
                plaintext.execSQL("DETACH DATABASE encrypted")
            }
        } finally {
            plaintext.close()
        }
        val encrypted = CipherDatabase.openDatabase(
            destination.absolutePath,
            passphrase,
            null,
            CipherDatabase.OPEN_READWRITE,
            { _, error -> throw error },
            null,
        )
        encrypted.use {
            it.execSQL("PRAGMA user_version = $databaseVersion")
        }
    }

    private fun isEncryptedAndReadable(file: File): Boolean {
        if (!file.isFile || hasPlaintextHeader(file)) return false
        return try {
            System.loadLibrary("sqlcipher")
            val database = CipherDatabase.openDatabase(
                file.absolutePath,
                passphrase,
                null,
                CipherDatabase.OPEN_READONLY,
                { _, error -> throw error },
                null,
            )
            database.use {
                it.rawQuery("SELECT count(*) FROM sqlite_master", null).use { cursor ->
                    check(cursor.moveToFirst()) { "Encrypted database schema could not be read" }
                }
                // Unlike integrity_check, cipher_integrity_check returns no rows on success.
                val cipherIntegrityValid = it.rawQuery("PRAGMA cipher_integrity_check", null).use { cursor ->
                    !cursor.moveToFirst()
                }
                val sqliteIntegrityValid = it.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getString(0) == "ok" && !cursor.moveToNext()
                }
                cipherIntegrityValid && sqliteIntegrityValid
            }
        } catch (error: Exception) {
            Log.w(TAG, "Encrypted database verification failed", error)
            false
        }
    }

    private fun hasPlaintextHeader(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_HEADER.size) return false
        return file.inputStream().use { input ->
            val header = ByteArray(SQLITE_HEADER.size)
            input.read(header) == header.size && header.contentEquals(SQLITE_HEADER)
        }
    }

    private fun checkpointPlaintextDatabase(): Int {
        val database = android.database.sqlite.SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE,
            { throw android.database.sqlite.SQLiteDatabaseCorruptException("Plaintext migration source is corrupt") },
        )
        database.use {
            val version = it.rawQuery("PRAGMA user_version", null).use { cursor ->
                check(cursor.moveToFirst()) { "Could not read the existing database version" }
                cursor.getInt(0)
            }
            it.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) {
                    "Could not checkpoint the existing database before encryption"
                }
            }
            return version
        }
    }

    private fun deleteSidecars() {
        for (suffix in listOf("-wal", "-shm")) {
            val sidecar = File(databaseFile.parentFile, databaseFile.name + suffix)
            check(!sidecar.exists() || sidecar.delete()) {
                "Could not remove plaintext database sidecar ${sidecar.name}"
            }
        }
    }

    private fun backupFile() = File(databaseFile.parentFile, "${databaseFile.name}.plaintext-backup")
    private fun temporaryFile() = File(databaseFile.parentFile, "${databaseFile.name}.encrypted.tmp")

    private companion object {
        const val TAG = "EncryptedDbMigration"
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII)
    }
}
