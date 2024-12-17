import kr.toxicity.model.api.util.MathUtil
import kr.toxicity.model.util.toModel
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.PI

class GsonTest {
    @Test
    fun testJson() {
        val mosquito = javaClass.getResource("/mosquito.bbmodel")!!.openStream().toModel()
//        println(mosquito.animations()["idle"]!!.animator()["h_head"]!!.keyFrame.joinToString("\n") {
//            it.rotation()!!.x.toString()
//        })
    }
}