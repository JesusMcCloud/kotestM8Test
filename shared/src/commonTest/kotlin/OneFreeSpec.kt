import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OneFreeSpec: FreeSpec( {

    "Free Yay" {
        "A" shouldBe "A"
    }
    "Free Nay" {
        "A" shouldNotBe "A"
    }

})