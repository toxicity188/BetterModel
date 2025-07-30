import kr.toxicity.model.api.util.HttpUtil
import kr.toxicity.model.api.version.MinecraftVersion
import org.junit.jupiter.api.Test

class ModrinthTest {
    @Test
    fun testModrinth() {
        println(HttpUtil.versionList(MinecraftVersion(1, 21, 8)))
    }
}