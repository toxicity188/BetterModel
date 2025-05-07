import kr.toxicity.model.api.animation.VectorPoint
import kr.toxicity.model.api.util.VectorInterpolation
import kr.toxicity.model.api.util.VectorUtil
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class VectorTest {
    @Test
    fun testVector() {
        VectorUtil.putPoint(
            listOf(
                VectorPoint(Vector3f(1F, 2F, 3F), 1F, VectorInterpolation.CATMULLROM),
                VectorPoint(Vector3f(2F, 3F, 4F), 2F, VectorInterpolation.CATMULLROM),
                VectorPoint(Vector3f(3F, 4F, 5F), 3F, VectorInterpolation.CATMULLROM),
                VectorPoint(Vector3f(4F, 5F, 6F), 4F, VectorInterpolation.CATMULLROM),
            ),
            sortedSetOf(
                0F,
                0.5F,
                1F,
                1.5F,
                2F,
                2.5F,
                3F,
                3.5F,
                4F,
                4.5F
            )
        ).forEach {
            println(it)
        }
        val set = sortedSetOf(0.5F, 1F, 1.5F, 2F, 2.5F)
        VectorUtil.insertLerpFrame(
            set,
            0.25F
        )
        println(set.joinToString())
    }
}