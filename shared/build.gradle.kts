import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}


// Blocked by KSP.
///////// ITS EITHER THIS BLOCK OR DISABLING WAMSWASI //////////////////////////////////////////////////////////////////
tasks.configureEach {
    val n = name
    if (
        n.startsWith("compileTestKotlinWasmWasi") || // compilation task
        n.endsWith("WasmWasiTest")                   // execution task
    ) {
        enabled = false
    }
}
configurations
    .matching { it.name.endsWith("wasmWasiTestCompileClasspath") || it.name.endsWith("wasmWasiTestRuntimeClasspath") }
    .configureEach {
        exclude(
            group = "io.kotest",
            module = "kotest-framework-engine-wasm-wasi"
        )
        exclude(
            group = "io.kotest",
            module = "kotest-assertions-core-wasm-wasi"
        )
    }
// END BLOCKED BY SKP //////////////////////////////////////////////////////////////////////////////////////////////////


kotlin {

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }

    // for android tests, use the `kotestDebugUnitTest` or `kotestReleaseUnitTest` tasks
    androidTarget()

    // native has a bug - test reports aren't written at the module level, but they are at the aggregated root level
    // note: to run tests for native targets, use the standard gradle task eg linuxX64Test
    linuxX64()
    linuxArm64()
    mingwX64()

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

    // kotlin has no support for tests for androidNative targets, so they'll be skipped at runtime
    // it will also error saying the compiler is only at 2.0 - I cannot figure out how why since it's set to 2.2
    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    // to run JVM tests use either the gradle task `jvmTest` (requires engine dep) or the `jvmKotest` task (enhanced Kotest support)
    jvm()

    // for js tests run the `jsKotest` task
    js {
        outputModuleName = "@kotest/test"
        version = "1.0.0"
        binaries.executable()
        nodejs()
        browser { testTask { useKarma { useChromeHeadless() } } }
    }

    // won't do anything but won't fail either
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // for wasmJs tests run the `wasmJsKotest` task
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
        commonMain {
            dependencies {
                api(kotlin("stdlib"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotest.framework)
                implementation(libs.kotest.assert)
            }
        }
        // these deps should be added by gradle automatically from common main, but they don't seem to always work
        jsMain {
            dependencies {
                api(kotlin("stdlib-js"))
            }
        }
        // these deps should be added by gradle automatically from common main, but they don't seem to always work
        wasmJsMain {
            dependencies {
                api(kotlin("stdlib-wasm-js"))
            }
        }
        // these deps should be added by gradle automatically from common main, but they don't seem to always work
        wasmWasiMain {
            dependencies {
                api(kotlin("stdlib-wasm-wasi"))
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