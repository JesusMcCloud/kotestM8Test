import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TwoFunSpec : FunSpec({

    test("Yay with Fun") {
        "A" shouldBe "A"
    }
    test("Nay with Fun") {
        "A" shouldNotBe "A"
    }
})