import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    alias(libs.plugins.convention.standard)
    alias(libs.plugins.hangar)
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val minecraft = property("minecraft_version").toString()
val versionString = version.toString()
val groupString = group.toString()

val javadocJar = tasks.register<Jar>("javadocJar") {
    description = "Makes javadoc."
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier = "javadoc"
    from(layout.buildDirectory.dir("dokka/html").orNull?.asFile)
}

runPaper {
    disablePluginJarDetection()
}

val bettermodel get() = project(":platform:bettermodel-paper").tasks.named<Jar>("shadowJar").flatMap {
    it.archiveFile
}
val bettermodelTest get() = project(":test-plugin").tasks.jar.flatMap {
    it.archiveFile
}

runPaper.folia.registerTask {
    pluginJars(bettermodel, bettermodelTest)
    minecraftVersion(minecraft)
}

tasks {
    runServer {
        pluginJars(fileTree("plugins"))
        pluginJars(bettermodel, bettermodelTest)
        minecraftVersion(minecraft)
        downloadPlugins {
            hangar("ViaVersion", "5.11.0")
            hangar("ViaBackwards", "5.11.0")
            hangar("Skript", "2.16.1")
        }
    }
    build {
        finalizedBy(
            javadocJar
        )
    }
    jar {
        enabled = false
    }
}

hangarPublish {
    publications.register("plugin") {
        version = project.version as String
        id = "BetterModel"
        apiKey = System.getenv("HANGAR_API_TOKEN")
        val log = commitMessage()
        if (log != null) {
            changelog = log
            channel = "Snapshot"
        } else {
            changelog = rootProject.file("changelog/$versionString.md").readText()
            channel = "Release"
        }
        platforms {
            register(Platforms.PAPER) {
                jar = bettermodel
                platformVersions = SUPPORTED_VERSIONS
                dependencies {
                    hangar("SkinsRestorer") { required = false }
                }
            }
        }
    }
}
