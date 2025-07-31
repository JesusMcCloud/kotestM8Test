import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TwoFunSpec {

    @Test
    fun `Yay with Fun`() {
        assertEquals("A", "A")
    }

    @Test
    fun `Nay with Fun`() {
        assertNotEquals("A", "A")
    }
}