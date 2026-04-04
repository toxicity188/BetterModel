import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project

const val JAVA_VERSION = 25
val BUILD_NUMBER: String? = System.getenv("BUILD_NUMBER")

val Project.libs
    get() = rootProject.extensions.getByName("libs") as LibrariesForLibs

val LATEST_VERSION = listOf(
    "26.1",
    "26.1.1"
)

val SUPPORTED_VERSIONS = buildList {
    addAll(listOf(
        "1.21",
        "1.21.1",
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
        "1.21.8",
        "1.21.9",
        "1.21.10",
        "1.21.11",
    ))
    addAll(LATEST_VERSION)
}

val BUKKIT_LOADERS = listOf("spigot")
val PAPER_LOADERS = listOf("paper", "purpur", "folia")
