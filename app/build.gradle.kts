plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.0")
    }
}

android {
    namespace = "com.soundboost"
    compileSdk = 36
    
    ndkVersion = "28.0.12674087"  // 16 KB page size support

    defaultConfig {
        applicationId = "com.soundboost"
        minSdk = 24
        targetSdk = 36
        versionCode = 37
        versionName = "1.4.7"
        
        setProperty("archivesBaseName", "SoundSTBoost-v$versionName")
        
        // 16 KB page size support
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }
    
    signingConfigs {
        create("release") {
            if (keystoreProperties["storeFile"] != null) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Generate native debug symbols for Google Play Console
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    
    packaging {
        jniLibs {
            keepDebugSymbols += listOf("**/*.so")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    // Compose BOM for version alignment (updated to latest)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha02")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Kotlinx Serialization for Device Profiles (v1.4.0)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Glance for Widgets (v1.4.0)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // System UI Controller (updated to latest)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    
    // Glassmorphism & Advanced Effects
    implementation("dev.chrisbanes.haze:haze:1.2.0")
    implementation("dev.chrisbanes.haze:haze-materials:1.2.0")
    
    // Ktor for WebSocket server/client (Flash-Sync / Parti Modu)
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-cio:2.3.12")
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")
    
    // TensorFlow Lite for AI Stem Separation (GPU Accelerated)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
}
