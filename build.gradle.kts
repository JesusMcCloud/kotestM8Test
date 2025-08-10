import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}