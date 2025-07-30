package io.kotest.test

interface Platform {
    val name: String
}

fun getPlatform(): Platform = object : Platform {
    override val name: String = "Dummy"
}