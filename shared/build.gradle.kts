import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}


project.configurations.whenObjectAdded {
    if (name.startsWith("ksp") && name.endsWith("Test")) {
        val target = name.substring(3, name.length - 4).replaceFirstChar { it.lowercase() }
        val isJvm = name.lowercase().contains("jvm")

        project.logger.lifecycle("  >>[${project.name}] Adding Kotest symbol processor dependency to $name")
        if (!isJvm) project.dependencies.add(
            name,
            "io.kotest:kotest-framework-symbol-processor-jvm:${libs.versions.kotest.get()}"
        )

    }
}

project.afterEvaluate {
    tasks.configureEach {
        if (name == "kspTestKotlinJvm" || name == "kspDebugUnitTestKotlinAndroid" || name =="kspReleaseUnitTestKotlinAndroid") {
            enabled = false
        }
    }
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

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    //stile borked with M8
    //wasmWasi { nodejs() }

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
        commonMain.dependencies {
            // put your Multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotest.framework)
            implementation(libs.kotest.assert)
            implementation(libs.kotest.reporter)
            implementation(libs.kxio)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.junit)
        }
    }
}

val f = project.layout.buildDirectory.dir("test-results").get().dir("bolted-on").asFile.apply { mkdirs() }

val file = File("${project.layout.projectDirectory}/src/commonTest/kotlin/tempdir.kt")

file.createNewFile()
file.writer().use {
    it.write("val tempPath = \"${f.absolutePath}\"")
}

gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        task.finalizedBy("finalTask")
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
