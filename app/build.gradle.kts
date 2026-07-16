plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.airgf.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.airgf.app"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull() ?: 5
        versionName = System.getenv("ANDROID_VERSION_NAME") ?: "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val ghToken: String = run {
            val propsFile = rootProject.file("local.properties")
            if (!propsFile.exists()) {
                System.getenv("GH_API_TOKEN") ?: ""
            } else {
                var token = System.getenv("GH_API_TOKEN") ?: ""
                if (token.isEmpty()) {
                    propsFile.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("github.api.token=")) {
                            token = trimmed.removePrefix("github.api.token=").trim()
                        }
                    }
                }
                token
            }
        }
        val ghOwner: String = run {
            val propsFile = rootProject.file("local.properties")
            if (!propsFile.exists()) {
                System.getenv("GH_REPO_OWNER") ?: ""
            } else {
                var owner = System.getenv("GH_REPO_OWNER") ?: ""
                if (owner.isEmpty()) {
                    propsFile.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("github.repo.owner=")) {
                            owner = trimmed.removePrefix("github.repo.owner=").trim()
                        }
                    }
                }
                owner
            }
        }
        val ghRepo: String = run {
            val propsFile = rootProject.file("local.properties")
            if (!propsFile.exists()) {
                System.getenv("GH_REPO_NAME") ?: ""
            } else {
                var repo = System.getenv("GH_REPO_NAME") ?: ""
                if (repo.isEmpty()) {
                    propsFile.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("github.repo.name=")) {
                            repo = trimmed.removePrefix("github.repo.name=").trim()
                        }
                    }
                }
                repo
            }
        }
        buildConfigField("String", "GITHUB_API_TOKEN", "\"$ghToken\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"$ghOwner\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"$ghRepo\"")
        buildConfigField("String", "FEEDBACK_ASSETS_DIR", "\"feedback-assets\"")

        fun resolveSecret(envKey: String, propsKey: String): String {
            val fromEnv = System.getenv(envKey) ?: ""
            if (fromEnv.isNotEmpty()) return fromEnv
            val propsFile = rootProject.file("local.properties")
            var value = ""
            if (propsFile.exists()) {
                propsFile.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("$propsKey=")) {
                        value = trimmed.removePrefix("$propsKey=").trim()
                    }
                }
            }
            return value
        }

        val admobAppId = resolveSecret("ADMOB_APP_ID", "admob.app.id")
        val admobBannerUnitId = resolveSecret("ADMOB_BANNER_UNIT_ID", "admob.banner.unit.id")
        val admobInterstitialUnitId = resolveSecret("ADMOB_INTERSTITIAL_UNIT_ID", "admob.interstitial.unit.id")
        val admobRewardedUnitId = resolveSecret("ADMOB_REWARDED_UNIT_ID", "admob.rewarded.unit.id")

        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$admobBannerUnitId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"$admobInterstitialUnitId\"")
        buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$admobRewardedUnitId\"")

        manifestPlaceholders["admobAppId"] = admobAppId

        val reportEndpoint = resolveSecret("AIRGF_REPORT_ENDPOINT", "airgf.report.endpoint")
        buildConfigField("String", "REPORT_ENDPOINT", "\"$reportEndpoint\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    exclude(group = "com.google.firebase", module = "protolite-well-known-types")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.litertlm.android)
    // LiteRT-LM 0.14.0's Android binary calls SendChannel.close$default.
    // That bridge is present in coroutine 1.11 but absent from the 1.10 dependency graph.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0") {
        version { strictly("1.11.0") }
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0") {
        version { strictly("1.11.0") }
    }

    implementation(libs.okhttp)

    implementation(libs.mediapipe.tasks.vision) {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation("com.google.protobuf:protobuf-java:4.26.1")

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.sceneview)

    implementation(libs.coil.compose)
    implementation(libs.mlkit.genai.image.description)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.analytics)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing.ktx)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
}

