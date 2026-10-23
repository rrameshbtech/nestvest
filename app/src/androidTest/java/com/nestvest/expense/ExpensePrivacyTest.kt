package com.nestvest.expense

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.RoomExpenseRepository
import com.nestvest.expense.domain.ExpenseWrite
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.PrimaryProfile
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExpensePrivacyTest {
    @Suppress("DEPRECATION")
    @Test fun localCaptureRequiresNoNetworkOrBroadMediaPermissions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        for (forbidden in listOf(Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE,
            "android.permission.READ_MEDIA_IMAGES", "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM")) assertFalse(forbidden, forbidden in permissions)
        assertFalse(context.isDeviceProtectedStorage)
        val name = "expense-private-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            val result = RoomExpenseRepository(db).create(100,
                PrimaryProfile(UUID.randomUUID(), "Mira", "USD"))
            assertTrue(result is ExpenseWrite.Saved)
            assertTrue(context.getDatabasePath(name).canonicalPath.startsWith(context.dataDir.canonicalPath + "/"))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
