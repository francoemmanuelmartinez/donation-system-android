plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.francomartinez.donationsystem"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.francomartinez.donationsystem"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.viewpager2)
    implementation(libs.mpandroidchart)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mysql.connector)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}