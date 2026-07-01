plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

android {
    namespace = "com.flashsphere.privatednsqs"
    compileSdk = 37
    flavorDimensions += listOf("type")

    defaultConfig {
        applicationId = "com.flashsphere.privatednsqs"
        minSdk = 28
        targetSdk = 37
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        base.archivesName = "PrivateDnsQS-v${versionName}"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    signingConfigs {
        create("release") {
            storeFile = file(project.property("PRIVATE_DNS_QS_RELEASE_STORE_FILE") as String)
            storePassword = project.property("PRIVATE_DNS_QS_RELEASE_STORE_PASSWORD") as String
            keyAlias = project.property("PRIVATE_DNS_QS_RELEASE_KEY_ALIAS") as String
            keyPassword = project.property("PRIVATE_DNS_QS_RELEASE_KEY_PASSWORD") as String

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-SNAPSHOT"
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs["release"]
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    productFlavors {
        create("launcher") {
            dimension = "type"
            isDefault = true
        }
        create("nolauncher") {
            dimension = "type"
            applicationIdSuffix = ".nolauncher"
        }
    }
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    packaging {
        resources {
            excludes += listOf("META-INF", "META-INF/NOTICE")
        }
        dex {
            useLegacyPackaging = true
        }
    }
    lint {
        informational.addAll(setOf("GradleDependency", "NewerVersionAvailable"))
        warningsAsErrors = true
        abortOnError = true
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.addAll(
        rootProject.layout.projectDirectory.file("stability_config.conf"),
    )
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    implementation(libs.androidx.datastore.prefs)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.reorderable)

    implementation(libs.timber)
    implementation(libs.process.phoenix)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    compileOnly(libs.hidden.api.stub)
    implementation(libs.hidden.api.bypass)

    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.assertk)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.assertk)
    testImplementation(libs.jsonassert)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.named("lint") {
    dependsOn(
        "lintLauncherDebug",
        "lintNolauncherDebug",
        "lintLauncherRelease",
        "lintNolauncherRelease",
    )
}
