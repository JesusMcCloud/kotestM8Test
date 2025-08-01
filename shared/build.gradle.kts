import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.testballoon)
    alias(libs.plugins.serialization)

}
val f = File(System.getProperty("java.io.tmpdir") + "/test-results").apply { mkdirs() }

val file = File("${project.layout.projectDirectory}/src/commonTest/kotlin/tempdir.kt")

tasks.matching {  it.name.lowercase().endsWith("test") }.forEach {
    it.doLast {
        runCatching {
            logger.lifecycle("  >> Copying tests from ${f.absolutePath}")
            f.copyRecursively(layout.buildDirectory.asFile.get(), overwrite = true)
        }.getOrElse {
            project.logger.warn(" >> Copying tests from ${f.absolutePath} failed: ${it.message}")
        }
    }
}

file.createNewFile()
file.writer().use {
    it.write("val tempPath = \"${f.absolutePath}\"")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    macosArm64()
    macosX64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
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

    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    linuxX64()
    linuxArm64()
    mingwX64()

    jvm()

    wasmWasi { nodejs() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask { useKarma { useChromiumHeadless() } }
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
        commonMain.dependencies {


            // put your Multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.testballoon)
            implementation(libs.kotlin.test)
            implementation(libs.kxio)
            implementation(libs.kotlin.coroutines)
            implementation(libs.xmlutil)
            implementation(libs.atomicfu)
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
