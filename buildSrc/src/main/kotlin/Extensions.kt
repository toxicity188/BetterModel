import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project

const val JAVA_VERSION = 21
val BUILD_NUMBER: String? = System.getenv("BUILD_NUMBER")

val Project.libs
    get() = rootProject.extensions.getByName("libs") as LibrariesForLibs

val SUPPORTED_VERSIONS = listOf(
    "1.20.5",
    "1.20.6",
    "1.21",
    "1.21.1",
    "1.21.2",
    "1.21.3",
    "1.21.4",
    "1.21.5",
    "1.21.6",
    "1.21.7",
    "1.21.8",
    "1.21.9",
    "1.21.10",
    //"1.21.11"
)

val BUKKIT_LOADERS = listOf("spigot")
val PAPER_LOADERS = listOf("paper", "purpur", "folia")
