// The plugin versions are defined in the root build.gradle.kts, so we just apply them here.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt") // KAPT must be applied for Room
}


android {
    namespace = "za.ac.iie.TallyUp"
    compileSdk = 36 // Targeting the latest stable SDK

    defaultConfig {
        applicationId = "za.ac.iie.TallyUp"
        minSdk = 23
        this.targetSdk = 36 // Targeting the latest stable SDK
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

    // Configure for Java 1.8 compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Enable View Binding feature
    buildFeatures {
        viewBinding = true
    }
}

// *** CRITICAL KAPT FIX ***
// This configuration is crucial for stability with modern JDKs (Java 11+).
kapt {
    correctErrorTypes = true
    arguments {
        // This is a common workaround to force correct classpath resolution
        arg("nonExistent", "NonExistent")
    }
}


dependencies {
    // CORE (LATEST STABLE)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // LIFECYCLE & NAVIGATION (LATEST STABLE)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ROOM (Database) - LATEST STABLE
    val roomVersion = "2.8.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // KAPT processor for Room
    kapt("androidx.room:room-compiler:$roomVersion")

    // UTILS (LATEST STABLE)
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // TESTING (LATEST STABLE)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // MPAndroidChart (Chart Library)
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
}
