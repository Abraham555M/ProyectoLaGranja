plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.proyectolagranja"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rysoft.proyectolagranja"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    //AsyncHttpClient
    implementation("com.loopj.android:android-async-http:1.4.9")
    //Libreria de google para diseño
    implementation("com.google.android.material:material:1.12.0")
    //Para cargar imagenes
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation ("com.google.firebase:firebase-auth")
    //Swipe
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:23.4.1")

    implementation("com.google.firebase:firebase-appcheck-playintegrity:17.0.1")
    implementation("com.google.firebase:firebase-appcheck-debug")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}