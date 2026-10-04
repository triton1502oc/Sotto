import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.amh.sotto"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.amh.sotto"
        minSdk = 26
        targetSdk = 36
        versionCode = 21
        versionName = "1.6.0"
        androidResources.localeFilters += listOf("en", "in")
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    splits {
        abi {
            val isInvokingBundle = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) } ||
                project.hasProperty("disableSplits")
            isEnable = !isInvokingBundle
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties().apply {
        if (keystorePropertiesFile.exists()) {
            FileInputStream(keystorePropertiesFile).use { load(it) }
        }
    }

    val releaseStoreFilePath = System.getenv("KEYSTORE_FILE")
        ?: System.getenv("KEYSTORE_PATH")
        ?: keystoreProperties.getProperty("storeFile") // gitleaks:allow
    val releaseStorePassword = System.getenv("STORE_PASSWORD")
        ?: keystoreProperties.getProperty("storePassword") // gitleaks:allow
    val releaseKeyAlias = System.getenv("KEY_ALIAS")
        ?: keystoreProperties.getProperty("keyAlias") // gitleaks:allow
    val releaseKeyPassword = System.getenv("KEY_PASSWORD")
        ?: keystoreProperties.getProperty("keyPassword") // gitleaks:allow

    val releaseStoreFile = releaseStoreFilePath?.let { path ->
        val fileInApp = file(path)
        if (fileInApp.exists()) fileInApp else rootProject.file(path)
    }

    val isReleaseSigningConfigured = releaseStoreFile?.exists() == true &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (isReleaseSigningConfigured) {
                storeFile = releaseStoreFile // gitleaks:allow
                storePassword = releaseStorePassword // gitleaks:allow
                keyAlias = releaseKeyAlias // gitleaks:allow
                keyPassword = releaseKeyPassword // gitleaks:allow
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            } else if (System.getenv("REQUIRE_RELEASE_SIGNING") == "true") {
                throw GradleException("Release signing is required (REQUIRE_RELEASE_SIGNING=true) but release signing credentials are not configured or keystore file does not exist.")
            } else {
                logger.warn("Warning: Release signing not configured. Falling back to debug signing config.")
                signingConfig = signingConfigs.getByName("debug")
            }
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            @Suppress("DEPRECATION")
            val out = output as com.android.build.api.variant.impl.VariantOutputImpl
            val buildType = variant.buildType ?: "debug"
            val abi = output.filters.find {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier
            val abiSuffix = if (abi != null) "-$abi" else ""
            val buildTypeSuffix = if (buildType == "release") "" else "-$buildType"
            out.outputFileName = "Sotto-v${android.defaultConfig.versionName}${abiSuffix}${buildTypeSuffix}.apk"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.work.runtime.ktx)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("io.mockk:mockk:1.13.10")
  testImplementation("org.json:json:20240303")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Reorderable
  implementation("sh.calvin.reorderable:reorderable:3.1.0")

  // On-Device Translation
  implementation(libs.mlkit.translate)
}
