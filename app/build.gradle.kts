import java.util.Properties

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.giantnovadevs.mysamoney"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.giantnovadevs.mysamoney"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            // This reads the key and adds it to BuildConfig for "debug" builds
            val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
            buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        }
        getByName("release") {
            // This reads the key and adds it to BuildConfig for "release" builds
            val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
            buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
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
            excludes += "/META-INF/{INDEX.LIST}"  // Exclude the specific file (use / for root path)
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    configurations.all {
        resolutionStrategy {
            force("com.google.api-client:google-api-client:2.8.1")
            force("com.google.auto.value:auto-value:1.10.2")
            force("com.google.auto.value:auto-value-annotations:1.10.2")
        }
    }
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.litert.support.api)
    implementation(libs.androidx.work.runtime.ktx)
    val room_version = "2.8.3"

    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    implementation("androidx.room:room-ktx:$room_version")

    implementation("androidx.compose.ui:ui:1.7.3")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.compose.material:material-icons-extended-android:1.7.3")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.google.mlkit:text-recognition:16.0.0")

    // For CameraX (the modern way to handle the camera)
    // We'll use this instead of the old intent-based method
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    // Google Generative AI (for Gemini)
    implementation("com.google.genai:google-genai:1.0.0")

    // Optional: For Kotlin serialization (already handled internally)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 1. The Google Drive API
    implementation("com.google.apis:google-api-services-drive:v3-rev20240914-2.0.0")

    // Core Android extensions (downgraded to match Drive generation)
    implementation("com.google.api-client:google-api-client-android:2.0.0")

    // JSON parsing with Gson (aligned to 2.0.0)
    implementation("com.google.http-client:google-http-client-gson:2.0.0")

    // Gson factory for API client (aligned to 2.0.0)
    implementation("com.google.api-client:google-api-client-gson:2.0.0")

    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("com.google.auto.value:auto-value:1.10.2")
    annotationProcessor("com.google.auto.value:auto-value:1.10.2")
    ksp("com.google.auto.value:auto-value:1.10.2")

    // Annotations
    implementation("com.google.auto.value:auto-value-annotations:1.10.2")
    annotationProcessor("com.google.auto.value:auto-value-annotations:1.10.2")
    ksp("com.google.auto.value:auto-value-annotations:1.10.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-ads:23.2.0")

    implementation("com.android.billingclient:billing-ktx:7.0.0")
    implementation("com.itextpdf.android:kernel-android:7.2.5")
    implementation("com.itextpdf.android:layout-android:7.2.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}