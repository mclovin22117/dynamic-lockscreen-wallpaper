import java.util.Properties

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val spotifyCoverApiBaseUrl = (localProperties.getProperty("spotify.cover.api.baseUrl") ?: "").trim()
val spotifyCoverApiAuthToken = (localProperties.getProperty("spotify.cover.api.authToken") ?: "").trim()
val spotifyCoverApiBaseUrlDebug = (localProperties.getProperty("spotify.cover.api.baseUrl.debug") ?: spotifyCoverApiBaseUrl).trim()
val spotifyCoverApiAuthTokenDebug = (localProperties.getProperty("spotify.cover.api.authToken.debug") ?: spotifyCoverApiAuthToken).trim()
val spotifyCoverApiBaseUrlRelease = (localProperties.getProperty("spotify.cover.api.baseUrl.release") ?: spotifyCoverApiBaseUrl).trim()
val spotifyCoverApiAuthTokenRelease = (localProperties.getProperty("spotify.cover.api.authToken.release") ?: spotifyCoverApiAuthToken).trim()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dynamiclock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dynamiclock"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SPOTIFY_COVER_API_BASE_URL", "\"\"")
        buildConfigField("String", "SPOTIFY_COVER_API_AUTH_TOKEN", "\"\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "SPOTIFY_COVER_API_BASE_URL", "\"$spotifyCoverApiBaseUrlDebug\"")
            buildConfigField("String", "SPOTIFY_COVER_API_AUTH_TOKEN", "\"$spotifyCoverApiAuthTokenDebug\"")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "SPOTIFY_COVER_API_BASE_URL", "\"$spotifyCoverApiBaseUrlRelease\"")
            buildConfigField("String", "SPOTIFY_COVER_API_AUTH_TOKEN", "\"$spotifyCoverApiAuthTokenRelease\"")
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}