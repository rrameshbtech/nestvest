package com.nestvest.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.nestvest.R
import com.nestvest.profile.data.ProfileDatabase
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class ProfilePrivacyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Suppress("DEPRECATION")
    @Test fun runtimePermissionsComponentsAndDatabaseStayPrivate() {
        val info = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
                PackageManager.GET_PROVIDERS or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS)
        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertTrue(info.providers.orEmpty().none { it.exported })
        assertTrue(info.services.orEmpty().filter { it.exported }.all {
            it.name == "androidx.work.impl.background.systemjob.SystemJobService" &&
                it.permission == "android.permission.BIND_JOB_SERVICE"
        })
        assertTrue(info.receivers.orEmpty().filter { it.exported }.all {
            it.name in setOf("androidx.profileinstaller.ProfileInstallReceiver",
                "androidx.work.impl.diagnostics.DiagnosticsReceiver") && it.permission == Manifest.permission.DUMP
        })
        // These two activities are supplied only by the debug preview/test-manifest dependencies.
        val allowedActivities = setOf("com.nestvest.MainActivity",
            "androidx.compose.ui.tooling.PreviewActivity", "androidx.activity.ComponentActivity")
        assertTrue(info.activities.orEmpty().filter { it.exported }.all { it.name in allowedActivities })
        assertFalse(context.isDeviceProtectedStorage)
        val name = "private-${UUID.randomUUID()}.db"
        val database = ProfileDatabase.open(context, name)
        try {
            database.openHelper.writableDatabase
            assertTrue(context.getDatabasePath(name).canonicalPath.startsWith(context.dataDir.canonicalPath + "/"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun backupConfigurationExcludesCloudAndLimitsLocalMigration() {
        context.resources.getXml(R.xml.backup_rules).use { xml ->
            var includes = 0
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG && xml.name == "include") {
                    includes++
                    val domain = xml.getAttributeValue(null, "domain")
                    assertTrue(domain == "database" ||
                        (domain == "file" && xml.getAttributeValue(null, "path") == "receipts/"))
                    assertEquals("deviceToDeviceTransfer", xml.getAttributeValue(null, "requireFlags"))
                }
                xml.next()
            }
            assertEquals(if (Build.VERSION.SDK_INT >= 28) 2 else 0, includes)
        }
        context.resources.getXml(R.xml.data_extraction_rules).use { xml ->
            var section = ""
            val excluded = mutableSetOf<String>()
            var transfers = 0
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG) {
                    if (xml.name in listOf("cloud-backup", "device-transfer")) section = xml.name
                    if (section == "cloud-backup" && xml.name == "exclude") excluded += xml.getAttributeValue(null, "domain")
                    if (section == "cloud-backup") assertNotEquals("include", xml.name)
                    if (section == "device-transfer" && xml.name == "include") {
                        transfers++
                        val domain = xml.getAttributeValue(null, "domain")
                        assertTrue(domain == "database" ||
                            (domain == "file" && xml.getAttributeValue(null, "path") == "receipts/"))
                    }
                }
                xml.next()
            }
            assertTrue(excluded.containsAll(listOf("root", "file", "database", "sharedpref", "external",
                "device_root", "device_file", "device_database", "device_sharedpref")))
            assertEquals(2, transfers)
        }
    }
}
