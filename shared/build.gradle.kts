import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotest)
  alias(libs.plugins.ksp)
}

kotlin {

  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = KotlinVersion.KOTLIN_2_2
  }

  androidTarget()

  macosArm64()
  macosX64()
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  watchosDeviceArm64()
  watchosSimulatorArm64()
  watchosX64()
  watchosArm32()
  watchosArm64()
  tvosSimulatorArm64()
  tvosX64()
  tvosArm64()

//  androidNativeX64()
//  androidNativeX86()
//  androidNativeArm32()
//  androidNativeArm64()

  linuxX64()
  linuxArm64()
//  mingwX64()

  jvm()

  js {
    nodejs()
    browser()
  }

//  @OptIn(ExperimentalWasmDsl::class)
//  wasmWasi {
//    nodejs()
//  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      testTask { useKarma { useChromeHeadless() } }
      val rootDirPath = project.rootDir.path
      val projectDirPath = project.projectDir.path
      commonWebpackConfig {
        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
          static = (static ?: mutableListOf()).apply {
            // Serve sources to debug inside browser
            add(rootDirPath)
            add(projectDirPath)
          }
        }
      }
    }
    nodejs()
  }

  sourceSets {
    commonTest {
      dependencies {
        implementation(libs.kotest.framework)
        implementation(libs.kotest.assert)
      }
    }
    jsMain {
      dependencies {
        api(kotlin("stdlib-js"))
      }
    }
    wasmJsMain {
      dependencies {
        api(kotlin("stdlib-wasm-js"))
      }
    }
  }
}

android {
  namespace = "io.kotest.test.shared"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
}