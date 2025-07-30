import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OneFreeSpec: FreeSpec( {

    "Yay" {
        "A" shouldBe "A"
    }
    "Nay" {
        "A" shouldNotBe "A"
    }

})