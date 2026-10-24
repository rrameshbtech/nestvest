package com.nestvest.kakeibo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestvest.profile.data.DatabaseKeyStore
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KakeiboPrivacyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun databasePassphraseIsWrappedAndItsWrappingKeyIsHeldByAndroidKeystore() {
        val passphrase = DatabaseKeyStore(context).getOrCreatePassphrase()
        val preferences = context.getSharedPreferences("database_key", Context.MODE_PRIVATE)
        val wrapped = preferences.getString("wrapped_passphrase", null)

        assertNotNull(wrapped)
        assertFalse(wrapped!!.contains(String(passphrase, StandardCharsets.US_ASCII)))
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertTrue(keyStore.containsAlias("nestvest.database.key.v1"))
    }
}
