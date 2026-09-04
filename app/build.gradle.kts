import com.github.jk1.license.filter.LicenseBundleNormalizer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    // No `org.jetbrains.kotlin.android` plugin: AGP 9's built-in Kotlin support covers it, and
    // applying it explicitly on top is deprecated (it re-triggers the legacy variant API AGP
    // 9's "new DSL" retired) — see the gradle.properties comment by android.newDsl's removal.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.license.report)
    alias(libs.plugins.roborazzi)
}

// T9.4: release signing credentials, loaded from a gitignored keystore.properties file --
// never from source. Falls back to debug signing (unusable for a real release, but keeps
// assembleRelease/bundleRelease working) when no keystore.properties exists yet, e.g. a fresh
// checkout before a real keystore has been generated. See keystore.properties.example.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "at.j0s.meyercard.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "at.j0s.meyercard.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 13
        versionName = "1.0.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // AGP writes a "Dependency metadata" block into the APK/AAB signing block by default (a
    // Play Console feature listing third-party dependencies, encrypted to a Google key).
    //
    // The APK must not carry it. F-Droid publishes this project's own signed APK rather than
    // an F-Droid-signed one (its metadata pairs Binaries with AllowedAPKSigningKeys), which
    // means F-Droid rebuilds the release from source and only ships it if that rebuild is
    // byte-identical to the APK on GitHub Releases. F-Droid's rebuild can never contain the
    // block -- its scanner rejects any signing block it doesn't recognise, which is how this
    // surfaced in the first place ("Found extra signing block 'Dependency metadata'").
    //
    // So this is deliberately unconditional. It used to be dropped only when F-Droid passed
    // -Pfdroid, which made F-Droid's APK and the published one differ by construction -- the
    // one thing reproducibility forbids.
    //
    // The bundle keeps it: the AAB goes only to Play Console, which is the consumer that
    // actually reads the block, and is never compared against a rebuild.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // AGP 8.3+ embeds the checkout's git revision in META-INF/version-control-info.
            // textproto. It reproduces only as long as F-Droid's rebuild sits on exactly the
            // same commit as the tagged release build -- true today, but it makes the APK's
            // bytes depend on someone else's checkout mechanics rather than on this source
            // tree, and F-Droid lists it as a known cause of verification failures. The
            // release tag already records which commit an APK came from, so nothing is lost
            // by leaving it out.
            vcsInfo {
                include = false
            }
            // Real signing when keystore.properties exists (T9.4); debug-signed fallback
            // otherwise so assembleRelease/bundleRelease keep working on a fresh checkout. A
            // debug-signed release build must never actually ship — see T9.4's own checklist.
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests {
            // Roborazzi (T2.4) renders through Robolectric, which needs real
            // resources/assets on its classpath to inflate anything Compose
            // touches — without this, Robolectric-backed tests fail before
            // they even reach the composable under test.
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                // Exposes data/original_cards.json to JVM tests without duplicating
                // it into a test-resources tree. The file becomes an app asset
                // properly in T3.1; until then this is how dataset-fixture tests
                // (e.g. CardGeometryTest) reach it. See docs/NEXT_STEPS.md T1.9.
                it.systemProperty(
                    "fechtkarte.originalCardsDataset",
                    rootProject.file("data/original_cards.json").absolutePath
                )
                // Without this, captureRoboImage() in MeyerSquareCardScreenshotTest is a
                // silent no-op under the plain `test`/`check`/`build` tasks — Roborazzi
                // only records or compares when driven by its own Gradle tasks
                // (recordRoborazziDebug, verifyRoborazziDebug, ...) or this property.
                // Found by noticing finalizeTestRoborazziDebug was SKIPPED and no PNGs
                // were written even though all 8 tests reported passing — the tests were
                // proving nothing. `verify`, not `record`: the commit gate should fail on
                // a rendering regression, not silently overwrite the accepted goldens.
                it.systemProperty("roborazzi.test.verify", "true")
                // The dedicated recordRoborazziDebug/verifyRoborazziDebug tasks read the
                // roborazzi { outputDir } extension below and pass it along as this same
                // property automatically; the plain `test` task doesn't, and silently fell
                // back to the default (gitignored) build/outputs/roborazzi — comparing
                // against a stale image left over from before outputDir was set, not the
                // committed golden. Has to be set explicitly here too.
                it.systemProperty(
                    "roborazzi.output.dir",
                    rootProject.file("app/src/testDebug/screenshots").absolutePath
                )
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // androidx.graphics:graphics-path (pulled in by Compose UI) ships prebuilt, already
        // stripped .so files for four ABIs. AGP strips native libraries again on their way
        // into the APK -- but only when it finds an NDK, and the result depends on which NDK
        // it finds. The release image has none, so the published APKs carry those bytes
        // verbatim: all four hashes in fechtkarte-1.0.2.apk match the ones inside
        // graphics-path-1.0.1.aar exactly. F-Droid's buildserver does ship NDKs, so its
        // rebuild would strip them and diverge from the APK it is meant to reproduce.
        //
        // Keeping the symbols makes the copy verbatim on both sides, which takes the NDK out
        // of the equation instead of trying to pin the same one in two build environments we
        // only control one of. Costs nothing in size -- upstream already stripped these.
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

// Kotlin 2.4's Gradle plugin turned the old `kotlinOptions { jvmTarget = "1.8" }`
// DSL into a hard compile error (it wants the compilerOptions DSL instead) —
// found by running the build, not by reading Kotlin's changelog first. Same
// bytecode target as before; only the way of expressing it changed.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // RoomCardRepositoryTest (src/testDebug/) defines its own @Database classes to exercise a
    // real schema-version migration without depending on the main build's BuildConfig.VERSION_CODE
    // - those need their own _Impl generated too, which the main-source-set `ksp` config above
    // doesn't cover.
    kspTestDebug(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // ViewModel & LiveData
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // JSON parsing — consumed starting T3.1 (the bundled historical-card dataset)
    implementation(libs.kotlinx.serialization.json)

    // Testing — JVM unit tests on JUnit 5 (Jupiter)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.konsist)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Roborazzi screenshot tests (T2.4) live under src/testDebug/, not
    // src/test/ — testDebugImplementation, not testImplementation. They run
    // on Robolectric under RobolectricTestRunner, which is JUnit 4
    // (@RunWith/@get:Rule) — bridged into the same JUnit Platform test task
    // via the Vintage engine. Debug-only because Robolectric's activity
    // resolution (`ActivityScenario.launch`, used by `ComposeTestRule`)
    // needs `android:debuggable="true"` in the merged manifest to resolve an
    // activity through an implicit MAIN/LAUNCHER intent at all — release
    // unit tests failed every test with "Unable to resolve activity for
    // Intent" until this was scoped to the debug variant, regardless of
    // which Gradle configuration declared the dependencies or which host
    // activity (bare ComponentActivity, or Roborazzi's own RoborazziActivity)
    // the test used. Confirmed by diffing the merged
    // testDebugUnitTest/testReleaseUnitTest manifests — `debuggable="true"`
    // was the only relevant difference. Testing the same UI code against
    // both build types would be redundant here anyway: isMinifyEnabled is
    // false for release, so app bytecode doesn't differ between variants.
    testDebugImplementation(libs.junit4)
    testDebugRuntimeOnly(libs.junit.vintage.engine)
    testDebugImplementation(libs.roborazzi)
    testDebugImplementation(libs.roborazzi.compose)
    testDebugImplementation(libs.roborazzi.junit.rule)
    testDebugImplementation(libs.robolectric)
    testDebugImplementation(libs.androidx.test.junit)
    testDebugImplementation(platform(libs.androidx.compose.bom))
    testDebugImplementation(libs.androidx.compose.ui.test.junit4)

    // androidTest instrumentation stays on AndroidX JUnit4 — ui-test-junit4 requires it
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Default outputDir (build/outputs/roborazzi) is gitignored — golden images have to
// live somewhere `git` actually tracks to protect against regressions across commits.
// Colocated with the screenshot test itself, matching its src/testDebug/ scope.
roborazzi {
    outputDir.set(file("src/testDebug/screenshots"))
}

// FOSS readiness gate (decision D8, docs/NEXT_STEPS.md T0.3). Raw POM license text
// varies by publisher for the same licence ("The Apache Software License, Version
// 2.0" vs "Apache-2.0" vs "Apache License, Version 2.0" — all three appear in this
// project's own dependency graph for plain Apache-2.0), so normalise before checking
// rather than trying to allowlist every publisher's phrasing.
licenseReport {
    filters = arrayOf(LicenseBundleNormalizer())
    allowedLicensesFile = rootProject.layout.projectDirectory.file("config/allowed-licenses.json")
}

tasks.named("check") {
    dependsOn("checkLicense")
}
