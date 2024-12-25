import kr.toxicity.model.util.toModel
import org.junit.jupiter.api.Test

class GsonTest {
    @Test
    fun testJson() {
        val mosquito = javaClass.getResource("/mosquito.bbmodel")!!.openStream().toModel()
//        println(mosquito.animations()["idle"]!!.animator()["h_head"]!!.keyFrame.joinToString("\n") {
//            it.rotation()!!.x.toString()
//        })
    }
}