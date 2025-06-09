import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project

const val JAVA_VERSION = 21
val BUILD_NUMBER: String? = System.getenv("BUILD_NUMBER")

val Project.libs
    get() = rootProject.extensions.getByName("libs") as LibrariesForLibs