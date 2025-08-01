package io.kotest.provided

import io.kotest.common.platform
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.kotest.framework.multiplatform.JUnitXmlWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import tempPath
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf<Extension>(Reporter)
}

object Reporter : AfterTestListener, AfterProjectListener, AfterSpecListener {
    @OptIn(ExperimentalTime::class)
    val collector = JUnitXmlWriter(
        Clock.System,
        includeStackTraces = true
    )

    private val mutex = Mutex()
    val tests = mutableMapOf<TestCase, TestResult>()

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        mutex.withLock {
            tests[testCase] = result
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        mutex.withLock {
            runCatching {
                val path = Path(tempPath)
                SystemFileSystem.createDirectories(path)
                val xml = collector.writeXml(spec, tests)
                val sink = SystemFileSystem.sink(
                    Path(
                        path,
                        "TEST-${platform.name}-" + Random.nextBytes(4).toHexString() + "-${spec::class.simpleName}.xml"
                    ), append = false
                ).buffered()
                sink.writeString(xml)
                sink.flush()
                sink.close()
                tests.clear()
            }.getOrElse { it.printStackTrace() }
        }
    }
}