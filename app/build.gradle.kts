import org.gradle.api.tasks.Copy
import java.io.File

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.driversvans"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.driversvans"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { compose = true }

    // Kotlin 1.9.23 ↔ Compose compiler 1.5.11
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Required for Apache POI (and some other libs) on Android
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

// --- Copy APK tasks ----------------------------------------------------------

val customDestDir: File = file("C:/AutoSyncToPhone/DriverVans")

val debugApkProvider = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
val releaseApkProvider = layout.buildDirectory.file("outputs/apk/release/app-release.apk")

val copyDebugApkToCustomFolder by tasks.registering(Copy::class) {
    from(debugApkProvider)
    into(customDestDir)
    doLast {
        logger.lifecycle("✅ Debug APK copied to: ${customDestDir.absolutePath}")
    }
}

val copyReleaseApkToCustomFolder by tasks.registering(Copy::class) {
    from(releaseApkProvider)
    into(customDestDir)
    doLast {
        logger.lifecycle("✅ Release APK copied to: ${customDestDir.absolutePath}")
    }
}

// Wire AFTER AGP creates variant tasks (avoids "assembleDebug not found")
afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy(copyDebugApkToCustomFolder)
    tasks.findByName("assembleRelease")?.finalizedBy(copyReleaseApkToCustomFolder)
}

dependencies {
    // Compose BOM keeps UI libs in sync
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Media (keep if you actually use it; otherwise remove)
    implementation("androidx.media3:media3-exoplayer:1.8.0")

    // --- Excel: Apache POI (choose POI; remove JXL) ---
    implementation("org.apache.poi:poi:5.2.5")        // .xls (HSSF)
    implementation("org.apache.poi:poi-ooxml:5.2.5")  // .xlsx (XSSF)

    // REMOVE this to avoid extra weight / confusion:
    // implementation("net.sourceforge.jexcelapi:jxl:2.6.12")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

// Optional, but nice for Room schemas & incremental KSP
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
