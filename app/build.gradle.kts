plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.fintechapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fintechapp"
        minSdk = 24
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildToolsVersion = "35.0.1"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase BOM (must be declared first)
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Firebase Firestore


    // Firebase Core (for Firebase initialization)
    implementation("com.google.firebase:firebase-common-ktx")
    implementation(libs.firebase.database)
    // Firebase UI Database (for FirebaseRecyclerAdapter)
    implementation("com.firebaseui:firebase-ui-database:8.0.2")
    implementation(libs.foundation.layout.android)
    implementation(libs.play.services.auth)
    implementation(libs.google.firebase.auth)


    // Unit Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
