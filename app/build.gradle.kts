import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

val envProperties = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.inputStream().use { envProperties.load(it) }
}

fun getSecret(key: String, defaultValue: String = ""): String {
    val envValue = System.getenv(key)
    if (!envValue.isNullOrBlank()) {
        return envValue
    }
    val propValue = envProperties.getProperty(key) as? String
    if (!propValue.isNullOrBlank()) {
        return propValue
    }
    return defaultValue
}

android {
    namespace = "com.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.questionbank.v1.zelcbr"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "APP_UPDATE_REPO", "\"${getSecret("APP_UPDATE_REPO", "myslv409-debug/OTSNGG")}\"")
            buildConfigField("String", "APP_UPDATE_TOKEN", "\"${getSecret("APP_UPDATE_TOKEN")}\"")
            buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
            buildConfigField("String", "APP_GITHUB_SHA", "\"${getSecret("APP_GITHUB_SHA")}\"")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "APP_UPDATE_REPO", "\"${getSecret("APP_UPDATE_REPO", "myslv409-debug/OTSNGG")}\"")
            buildConfigField("String", "APP_UPDATE_TOKEN", "\"${getSecret("APP_UPDATE_TOKEN")}\"")
            buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
            buildConfigField("String", "APP_GITHUB_SHA", "\"${getSecret("APP_GITHUB_SHA")}\"")
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
    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/AL2.0"
            excludes += "/META-INF/LGPL2.1"
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    implementation(libs.coil.compose)
    implementation(libs.commons.net)
    implementation(libs.jcifs.ng)
    implementation(libs.play.services.auth)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
