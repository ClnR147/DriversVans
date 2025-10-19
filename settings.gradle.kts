pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.1.2"
        id("com.android.library") version "8.1.2"

        kotlin("android") version "1.9.23"
        kotlin("jvm") version "1.9.23"
        kotlin("plugin.serialization") version "1.9.23"

        id("com.google.devtools.ksp") version "1.9.23-1.0.20"
    }
    // 🔒 Force any Kotlin/KSP plugin request to these exact versions
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                useVersion("1.9.23")
            }
            if (requested.id.id == "com.google.devtools.ksp") {
                useVersion("1.9.23-1.0.20")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "DriversVans"
include(":app")
