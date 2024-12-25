import kr.toxicity.model.api.data.blueprint.BlueprintAnimator
import kr.toxicity.model.api.data.raw.Datapoint
import kr.toxicity.model.api.data.raw.KeyframeChannel
import kr.toxicity.model.api.data.raw.ModelKeyframe
import org.junit.jupiter.api.Test

class AnimationTest {
    @Test
    fun testAnimation() {
        val builder = BlueprintAnimator.Builder()
        builder.addFrame(ModelKeyframe(KeyframeChannel.ROTATION, listOf(Datapoint(10F, 0F, 0F)), 0.5F))
        builder.addFrame(ModelKeyframe(KeyframeChannel.ROTATION, listOf(Datapoint(15F, 0F, 0F)), 1.5F))
        builder.addFrame(ModelKeyframe(KeyframeChannel.ROTATION, listOf(Datapoint(10F, 0F, 0F)), 2.5F))
        builder.addFrame(ModelKeyframe(KeyframeChannel.ROTATION, listOf(Datapoint(0F, 0F, 0F)), 3F))
        //println(builder.build("test").keyFrame.joinToString("\n") { movement -> movement.rotation()!!.x.toString() })
    }
}