plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nestvest"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.nestvest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    buildTypes {
        debug { isPseudoLocalesEnabled = true }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets["androidTest"].assets.directories.add("$projectDir/schemas")
    lint { abortOnError = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.savedstate)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.coroutines)
    implementation(libs.work.runtime)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.runner)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.compose.accessibility)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.work.testing)
    androidTestImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
