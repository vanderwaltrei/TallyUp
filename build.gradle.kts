// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android Gradle Plugin (AGP) - Updated to 8.7.3 for better compatibility
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false

    // Kotlin Gradle Plugin (KGP) - Using 2.0.21 for stability with KSP
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false

    // KSP - Modern replacement for KAPT
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

// Removed KAPT - migrating to KSP for better Kotlin support