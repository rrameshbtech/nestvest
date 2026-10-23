package com.nestvest.profile

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.nestvest.R
import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.*
import com.nestvest.profile.ui.theme.NestVestTheme
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileLocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun englishAndUntranslatedLanguagesHaveCompleteEnglishFallback() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (locale in listOf(Locale.ENGLISH, Locale.FRANCE)) {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(locale)
            })
            assertEquals("Create my profile", localized.getString(R.string.create_profile))
            assertEquals("Your name", localized.getString(R.string.name_label))
            assertEquals("Saved on this device", localized.getString(R.string.saved_status))
        }
        assertNotEquals(CurrencyCatalog.label("INR", Locale.ENGLISH), CurrencyCatalog.label("INR", Locale.FRANCE))
    }

    @Test fun localeMatrixPreservesUnicodeNameAndCurrencyIdentity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val locale = mutableStateOf(Locale.ENGLISH)
        val profile = uiProfile.copy(name = "மீரா 李", preferredCurrency = "INR")
        compose.setContent {
            val config = Configuration(context.resources.configuration).apply { setLocale(locale.value) }
            val localized = context.createConfigurationContext(config)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides config,
                LocalLayoutDirection provides if (config.layoutDirection == 1) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                NestVestTheme { ProfileContent(ProfileUiState(stage = ProfileStage.SAVED, profile = profile)) {} }
            }
        }
        for (language in listOf(Locale.ENGLISH, Locale.FRANCE, Locale.forLanguageTag("en-XA"), Locale.forLanguageTag("ar-XB"))) {
            compose.runOnIdle { locale.value = language }
            compose.onNodeWithText(profile.name).assertExists()
            compose.onNode(hasText("INR", substring = true)).performScrollTo().assertIsDisplayed()
            assertEquals("INR", profile.preferredCurrency)
        }
    }
}
