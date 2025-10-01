// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android Gradle Plugin (AGP) - Locked to 8.11.1 to match the max supported version
    // mentioned in the error message, ensuring stability with your current environment.
    id("com.android.application") version "8.11.1" apply false
    id("com.android.library") version "8.11.1" apply false

    // Kotlin Gradle Plugin (KGP) and KAPT - Updated to the latest stable version 2.2.20
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.20" apply false
}

// Add configuration for dependency resolution if required (often found in settings.gradle.kts or older build files)
// If you see issues resolving dependencies, you might need to add repository configuration here:
// allprojects {
//     repositories {
//         google()
//         mavenCentral()
//     }
// }
