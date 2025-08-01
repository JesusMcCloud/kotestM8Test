package at.asitplus.test


import de.infix.testBalloon.framework.*
import de.infix.testBalloon.framework.internal.printlnFixed
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import tempPath
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.measureTime


class MyTestSession : TestSession(testConfig = DefaultConfiguration.traversal(JUnitXmlReporter))

/* ---------- platform hook ---------- */
private fun writeXmlFile(xml: String, filename: String) = try {
    val path = Path(tempPath, filename)
    println(" >> Test report will be written to $path")
    val sink = SystemFileSystem.sink(path, append = false).buffered()
    sink.writeString(xml)
    sink.close()

} catch (e: Exception) {
    e.printStackTrace()
}

private fun Double.toThreeDecimals(): String {
    val multiplied = this * 1000
    val rounded = round(multiplied) / 1000
    return rounded.toString()
}


private object DoubleSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("JunitXMLDouble", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeString(value.toThreeDecimals())
    }

    override fun deserialize(decoder: Decoder): Double {
        return decoder.decodeString().toDouble()
    }

}

@Serializable
@XmlSerialName("testsuite", "", "")
private data class TestSuite(
    @XmlElement(false) val name: String,
    @XmlElement(false) val tests: Int,
    @XmlElement(false) val failures: Int,
    @XmlElement(false) val errors: Int,
    @XmlElement(false) val skipped: Int,
    @XmlElement(false) val timestamp: String,
    @XmlElement(false) @Serializable(with = DoubleSerializer::class) val time: Double,
    @XmlElement val cases: List<TestCaseXml>
)

@Serializable
@XmlSerialName("testcase", "", "")
private data class TestCaseXml(
    @XmlElement(false) val classname: String,
    @XmlElement(false) val name: String,
    @XmlElement(false) @Serializable(with = DoubleSerializer::class) val time: Double,
    val failure: Failure? = null,
    val error: ErrorTag? = null,
    val skipped: Skipped? = null
)

@Serializable
@XmlSerialName("failure", "", "")
private data class Failure(
    @XmlElement(false) val message: String,
    @XmlElement(false) val type: String,
    @XmlValue val stack: String
)

@Serializable
@XmlSerialName("error", "", "")
private data class ErrorTag(
    @XmlElement(false) val message: String,
    @XmlElement(false) val type: String,
    @XmlValue val stack: String
)

@Serializable
@XmlSerialName("skipped", "", "")
private class Skipped

/* ---------- Kotest listener that writes the file at engine stop ---------- */
object JUnitXmlReporter : TestExecutionTraversal {

    private val reportStart = atomic<TimeSource.Monotonic.ValueTimeMark?>(null)
    private val lock = reentrantLock()

    // These may be mutated only while `lock` is held.
    private var testCount = 0
    private var testFailureCount = 0
    private var cumulativeTestDuration = 0.seconds
    private var slowestTestDuration: Duration = Duration.ZERO
    private var slowestTestPath = "(none)"
    private val threadIdsUsed = mutableSetOf<ULong>()

    val bySpec = mutableMapOf<String, MutableList<Triple<TestElement, Duration, Throwable?>>>()

    override suspend fun aroundEach(testElement: TestElement, elementAction: suspend TestElement.() -> Unit) {
        val isReportRootElement = reportStart.compareAndSet(null, TimeSource.Monotonic.markNow())

        if (testElement is Test) {
            var testResult: Throwable? = null
            measureTime {
                try {
                    testElement.elementAction()
                } catch (throwable: Throwable) {
                    testResult = throwable
                }
            }.also { duration ->
                lock.withLock {
                    testCount++
                    if (testResult != null) testFailureCount++

                    var root = testElement
                    while (root.testElementParent != null)
                        root = root.testElementParent!!

                    val current = bySpec.getOrElse(root.testElementPath) { mutableListOf() }
                    current += Triple(testElement, duration, testResult)
                    bySpec[root.testElementPath] = current

                    cumulativeTestDuration += duration
                    if (duration > slowestTestDuration) {
                        slowestTestPath = testElement.testElementPath
                        slowestTestDuration = duration
                    }

                    threadIdsUsed.add(testPlatform.threadId())
                }
            }
            if (testResult != null) throw testResult
        } else {
            testElement.elementAction()
        }


        if (isReportRootElement) {
            val elapsedTime = reportStart.value!!.elapsedNow()
            printlnFixed(
                "${testElement.testElementPath}[${testPlatform.displayName}]: ran $testCount test(s)" +
                        " on ${threadIdsUsed.size} thread(s) in $elapsedTime," +
                        " cumulative test duration: $cumulativeTestDuration"
            )
            if (slowestTestDuration != Duration.ZERO) {
                printlnFixed(
                    "\tThe slowest test '$slowestTestPath' took $slowestTestDuration."
                )
            }
            writeReport(testElement as de.infix.testBalloon.framework.TestSuite)
        }

    }


    fun writeReport(root: de.infix.testBalloon.framework.TestSuite) {

        val suites = lock.withLock {
            bySpec.map { (spec, pairs) ->
                var fails = 0;
                var skips = 0
                val cases = pairs.map { (tc, res, throwable) ->

                    val secs = res.toDouble(DurationUnit.MILLISECONDS) / 1_000

                    if (throwable != null) fails++

                    if (tc.testElementIsEnabled) skips++
                    val bld = mutableListOf<String>()

                    val name = tc.testElementPath.substring(0, root.testElementPath.length)
                    TestCaseXml(
                        classname = spec,
                        name = "[${testPlatform.displayName}] $name",
                        time = secs,
                        failure = throwable?.let {
                            Failure(
                                it.message ?: "",
                                it::class.simpleName ?: "",
                                it.stackTraceToString()
                            )
                        },/*TODO error/failue cas split?*/
                        skipped = if (!tc.testElementIsEnabled) Skipped() else null
                    )
                }
                TestSuite(
                    spec,
                    cases.size,
                    0, /*TODO error failure cas split*/
                    fails,
                    skips,
                    reportStart.value!!.toString(),
                    cases.sumOf { it.time },
                    cases
                )
            }
        }

        suites.forEach {
            val xml = XML {
                indentString = "  "
                xmlVersion = XmlVersion.XML10
            }.encodeToString(it)

            val name = it.name.substringBefore(" ").replace('/','_').replace('\\','_')
            writeXmlFile(
                xml,
                "TEST-${testPlatform.displayName.substringBefore(" ")}-${name}-${Random.nextBytes(4).toHexString()}.xml"
            )
        }
    }

}