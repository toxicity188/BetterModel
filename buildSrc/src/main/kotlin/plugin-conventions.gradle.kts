plugins {
    id("bukkit-conventions")
    id("modrinth-conventions")
    id("com.gradleup.shadow")
}

val shade: Configuration = configurations.getByName("shade")
val versionString = version.toString()
val groupString = group.toString()
val classifier: String = project.name.substringAfterLast('-')

dependencies {
    compileOnly(project(":bettermodel-api"))
    compileOnly(project(":bettermodel-api:bettermodel-bukkit-api"))
    compileOnly(project(":bettermodel-core"))
    shade(project(":bettermodel-core:bettermodel-bukkit-core")) {
        exclude("org.jetbrains.kotlin")
    }
}

tasks {
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.WARN
        configurations.set(listOf(shade))
        manifest {
            attributes(mapOf(
                "Dev-Build" to (BUILD_NUMBER ?: -1),
                "Version" to versionString,
                "Author" to "toxicity188",
                "Url" to "https://github.com/toxicity188/BetterModel",
                "Created-By" to "Gradle $gradle",
                "Build-Jdk" to "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}",
                "Build-OS" to "${System.getProperty("os.arch")} ${System.getProperty("os.name")}"
            ) + libs.bundles.manifestLibrary.get().associate {
                "library-${it.name}" to it.version
            })
        }
        archiveBaseName = rootProject.name
        archiveClassifier = classifier
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        dependencies {
            exclude(dependency("org.jetbrains:annotations:26.0.2"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "$groupString.shaded.$pattern")
        }
        prefix("kotlin")
        prefix("kr.toxicity.library.armormodel")
        prefix("org.incendo.cloud")
        prefix("org.bstats")
        prefix("net.byteflux.libby")
    }
}

modrinth {
    uploadFile.set(tasks.shadowJar)
    dependencies {
        optional.project(
            "mythicmobs",
            "skinsrestorer"
        )
    }
}
