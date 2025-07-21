import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.metro)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.hossain.remotenotify"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.hossain.remotenotify"
        minSdk = 30
        targetSdk = 35
        versionCode = 20
        // 📣 Don't forget to update release notes! 🤓
        versionName = "1.16"

        // Read key or other properties from local.properties
        val localProperties =
            project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
                Properties().apply { load(it) }
            }
        val apiKey = System.getenv("EMAIL_API_KEY") ?: localProperties?.getProperty("EMAIL_API_KEY") ?: ""
        if (apiKey.isBlank()) {
            error("""
                EMAIL_API_KEY is not set in `local.properties`
                Please add 'EMAIL_API_KEY=your_api_key' to `local.properties` file.
            """.trimIndent())
        }
        buildConfigField("String", "EMAIL_API_KEY", "\"$apiKey\"")

        // Git commit hash to identify build source
        buildConfigField("String", "GIT_COMMIT_HASH", "\"${getGitCommitHash()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            firebaseCrashlytics {
                // https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android
                // https://developer.android.com/studio/debug/stacktraces
                // https://developer.android.com/tools/retrace
                // https://www.guardsquare.com/manual/tools/retrace
                mappingFileUploadEnabled = true
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        // https://developer.android.com/jetpack/androidx/releases/room#gradle-plugin
        schemaDirectory("$projectDir/schemas")
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    // Needed for Kover
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    // See https://kotlinlang.org/docs/gradle-compiler-options.html
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

// Kotlin Code Coverage - https://github.com/Kotlin/kotlinx-kover
kover {
    // Configure reports for the debug build variant
    // For now use default values, key tasks are
    // - koverHtmlReportDebug - Task to generate HTML coverage report for 'debug' Android build variant
    // - koverXmlReportDebug - Task to generate XML coverage report for 'debug' Android build variant
    reports {
        // filters for all report types of all build variants
        filters {
            excludes {
                androidGeneratedClasses()
                annotatedBy(
                    "*Composable",
                    "*Parcelize",
                    "*Preview",
                    "javax.annotation.processing.Generated"
                )
            }
        }

        variant("release") {
            // verification only for 'release' build variant
            verify {
                rule {
                    minBound(50)
                }
            }
        }
    }
}


dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.adaptive.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.properties)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.circuit.codegen.annotations)
    implementation(libs.circuit.foundation)
    implementation(libs.circuit.overlay)
    implementation(libs.circuitx.android)
    implementation(libs.circuitx.effects)
    implementation(libs.circuitx.gestureNav)
    implementation(libs.circuitx.overlays)
    implementation(libs.androidx.junit.ktx)
    ksp(libs.circuit.codegen)

    implementation(libs.timber)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.moshi)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Moshi
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    implementation(libs.eithernet)
    implementation(libs.eithernet.integration.retrofit)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // Google Play In-App Reviews
    implementation(libs.google.play.review)
    implementation(libs.google.play.review.ktx)

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Linting
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    lintChecks(libs.compose.lint.checks)

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // Testing
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.google.truth)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mock.webserver)
    testImplementation(libs.retrofit.mock.server)
    testImplementation(libs.robolectric)
}

ksp {
    // Circuit-KSP for Metro
    // https://slackhq.github.io/circuit/code-gen/
    arg("circuit.codegen.mode", "metro")
}

metro {
    // Enable Metro code generation for assisted injection factories.
    // https://zacsweers.github.io/metro/injection-types/#automatic-assisted-factory-generation
    // generateAssistedFactories.set(true)

    enableKotlinVersionCompatibilityChecks = true

    reportsDestination = layout.buildDirectory.asFile.get().resolve("reports/metro")
}


// Helper function to get the current Git commit hash
fun getGitCommitHash(): String {
    val processBuilder = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
    val output = File.createTempFile("git-short-commit-hash", "")
    processBuilder.redirectOutput(output)
    val process = processBuilder.start()
    process.waitFor()
    return output.readText().trim()
}