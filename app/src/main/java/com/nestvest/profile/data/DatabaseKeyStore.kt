package com.nestvest.profile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class DatabaseKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun getOrCreatePassphrase(): ByteArray {
        val encoded = preferences.getString(WRAPPED_PASSPHRASE, null)
        if (encoded != null) return unwrap(encoded)

        val passphrase = Base64.encodeToString(
            ByteArray(PASSPHRASE_BYTES).also(random::nextBytes),
            Base64.NO_WRAP,
        ).toByteArray(StandardCharsets.US_ASCII)
        val wrapped = wrap(passphrase)
        check(preferences.edit().putString(WRAPPED_PASSPHRASE, wrapped).commit()) {
            "Could not persist encrypted database key"
        }
        return passphrase
    }

    private fun wrap(passphrase: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase)
        return listOf(
            Base64.encodeToString(iv, Base64.NO_WRAP),
            Base64.encodeToString(ciphertext, Base64.NO_WRAP),
        ).joinToString(".")
    }

    private fun unwrap(encoded: String): ByteArray {
        val parts = encoded.split('.', limit = 2)
        require(parts.size == 2) { "Stored database key is malformed" }
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: error("Android Keystore database key is unavailable")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "database_key"
        const val WRAPPED_PASSPHRASE = "wrapped_passphrase"
        const val KEY_ALIAS = "nestvest.database.key.v1"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PASSPHRASE_BYTES = 32
        const val GCM_TAG_BITS = 128
    }
}
