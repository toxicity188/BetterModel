import io.papermc.hangarpublishplugin.model.Platforms
import java.time.LocalDateTime

plugins {
    alias(libs.plugins.standardConvention)
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.modrinth.minotaur") version "2.+"
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
    id("io.github.goooler.shadow") version "8.1.8"
}

val minecraft = property("minecraft_version").toString()

dependencies {
    implementation(project(":core"))
    fun searchAll(target: Project) {
        val sub = target.subprojects
        if (sub.isNotEmpty()) sub.forEach {
            searchAll(it)
        } else dokka(target)
    }
    searchAll(rootProject)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier = "javadoc"
    from(layout.buildDirectory.dir("dokka/html").orNull?.asFile)
}

val versionString = version.toString()
val groupString = group.toString()

tasks {
    runServer {
        pluginJars(fileTree("plugins"))
        pluginJars(project("test-plugin").tasks.jar.flatMap {
            it.archiveFile
        })
        version(minecraft)
        downloadPlugins {
            hangar("ViaVersion", "5.4.2")
            hangar("ViaBackwards", "5.4.2")
            hangar("Skript", "2.12.0")
        }
    }
    jar {
        finalizedBy(shadowJar)
    }
    shadowJar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "spigot",
                "Dev-Build" to (BUILD_NUMBER ?: -1),
                "Version" to versionString,
                "Author" to "toxicity188",
                "Url" to "https://github.com/toxicity188/BetterModel",
                "Created-By" to "Gradle $gradle",
                "Build-Jdk" to "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}",
                "Build-OS" to "${System.getProperty("os.arch")} ${System.getProperty("os.name")}",
                "Build-Date" to LocalDateTime.now().toString()
            )
        }
        archiveClassifier = ""
        dependencies {
            exclude(dependency("org.jetbrains:annotations:26.0.2"))
        }
        fun prefix(pattern: String) {
            relocate(pattern, "$groupString.shaded.$pattern")
        }
        exclude("LICENSE")
        prefix("kotlin")
        prefix("kr.toxicity.library")
        prefix("dev.jorel.commandapi")
        prefix("org.bstats")
    }
    build {
        finalizedBy(
            javadocJar
        )
    }
}

tasks.modrinth {
    dependsOn(tasks.modrinthSyncBody)
}

val supportedVersion = listOf(
    "1.20.3",
    "1.20.4",
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
    "1.21.8"
)

hangarPublish {
    publications.register("plugin") {
        version = project.version as String
        id = "BetterModel"
        apiKey = System.getenv("HANGAR_API_TOKEN")
        val log = System.getenv("COMMIT_MESSAGE")
        if (log != null) {
            changelog = log
            channel = "Snapshot"
        } else {
            changelog = rootProject.file("changelog/$versionString.md").readText()
            channel = "Release"
        }
        platforms {
            register(Platforms.PAPER) {
                jar = tasks.shadowJar.flatMap { it.archiveFile }
                platformVersions = supportedVersion
            }
        }
    }
}

modrinth {
    token = System.getenv("MODRINTH_API_TOKEN")
    projectId = "bettermodel"
    syncBodyFrom = rootProject.file("README.md").readText()
    val log = System.getenv("COMMIT_MESSAGE")
    if (log != null) {
        versionType = "beta"
        changelog = log
    } else {
        versionType = "release"
        changelog = rootProject.file("changelog/$versionString.md").readText()
    }
    uploadFile = tasks.shadowJar.get()
    additionalFiles = listOf(
        javadocJar.get()
    )
    versionName = "BetterModel $versionString"
    versionNumber = versionString
    gameVersions = supportedVersion
    loaders = listOf("bukkit", "spigot", "paper", "purpur", "folia")
}