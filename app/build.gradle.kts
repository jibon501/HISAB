plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ---------- ভার্সন ম্যানেজমেন্ট ----------
// GitHub Actions থেকে -PappVersionCode=<রান নম্বর> ও -PappVersionName=1.<রান নম্বর> আসে।
// লোকাল বিল্ডে ডিফল্ট: কোড 1, নাম 1.1
val appVersionCode: Int = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName: String = (project.findProperty("appVersionName") as String?) ?: "1.1"

// ---------- সাইনিং (এনভায়রনমেন্ট ভেরিয়েবল থেকে) ----------
val keystorePath: String? = System.getenv("KEYSTORE_PATH")
val hasKeystore: Boolean = keystorePath != null && file(keystorePath).exists()

android {
    namespace = "com.hisab.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hisab.app"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8: কোড ছোট করা + অবফাস্কেশন + অব্যবহৃত রিসোর্স বাদ
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
}
