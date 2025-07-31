import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OneFreeSpec {
    @Test
    fun `Free Yay`() {
        assertEquals("A", "A")
    }

    @Test
    fun `Free Nay`() {
        assertNotEquals("A", "A")
    }
}