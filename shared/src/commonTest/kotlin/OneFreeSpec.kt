import de.infix.testBalloon.framework.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val OneFreeSpec by testSuite {
    test("Free Yay") {
        assertEquals("A", "A")
    }

    test("Free Nay") {
        assertNotEquals("A", "A")
    }
}