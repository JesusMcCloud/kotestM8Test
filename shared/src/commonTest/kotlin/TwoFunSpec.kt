import de.infix.testBalloon.framework.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val TwoFunSpec by testSuite {

    test("Yay with Fun") {
        assertEquals("A", "A")
    }

    test("Nay with Fun") {
        assertNotEquals("A", "A")
    }
}