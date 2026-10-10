plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.leninasto.ruleapp.macrobenchmark"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":app"
    buildTypes {
        create("benchmark") { isDebuggable = true; signingConfig = signingConfigs.getByName("debug"); matchingFallbacks += listOf("release") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    experimentalProperties["android.experimental.self-instrumenting"] = true
}
androidComponents { beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" } }
dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation(libs.androidx.junit)
}
