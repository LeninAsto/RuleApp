import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val appVersion = rootProject.file("ver.txt").readText().trim()
val versionParts = Regex("(\\d+)\\.(\\d+)\\.(\\d+)(?:_beta)?").matchEntire(appVersion)
    ?: error("ver.txt debe contener una versión como 0.9.1_beta")
val appVersionCode = versionParts.groupValues[1].toInt() * 1_000_000 +
    versionParts.groupValues[2].toInt() * 10_000 + versionParts.groupValues[3].toInt() * 100 +
    if (appVersion.endsWith("_beta")) 1 else 99
val signingProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signingValue(name: String): String? = providers.environmentVariable("RULEAPP_${name.uppercase()}").orNull ?: signingProperties.getProperty(name)
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { !signingValue(it).isNullOrBlank() }
val targetAbi = providers.gradleProperty("targetAbi").orNull ?: "universal"
require(targetAbi in listOf("universal", "arm64-v8a", "armeabi-v7a", "x86", "x86_64")) { "ABI desconocida" }

android {
    namespace = "com.leninasto.ruleapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.leninasto.ruleapp"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
        if (targetAbi != "universal") ndk { abiFilters += targetAbi }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) create("ownerRelease") {
            storeFile = rootProject.file(signingValue("storeFile")!!)
            storePassword = signingValue("storePassword")
            keyAlias = signingValue("keyAlias")
            keyPassword = signingValue("keyPassword")
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("ownerRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".benchmark"
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3.stable)
    implementation("com.google.android.material:material:1.13.0")
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.browser)
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.viewmodel.compose)
    implementation(libs.androidx.viewmodel.savedstate)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)

    // Networking & Parsing
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.scalars)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)

    // Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.coroutines.test)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.google.code.gson)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
