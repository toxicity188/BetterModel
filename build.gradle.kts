import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    alias(libs.plugins.convention.standard)
    alias(libs.plugins.minotaur) apply false
    alias(libs.plugins.shadow)
    alias(libs.plugins.hangar)
    id("xyz.jpenilla.run-paper") version "3.0.0"
}

val minecraft = property("minecraft_version").toString()
val versionString = version.toString()
val groupString = group.toString()

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier = "javadoc"
    from(layout.buildDirectory.dir("dokka/html").orNull?.asFile)
}

runPaper {
    disablePluginJarDetection()
}

tasks {
    runServer {
        pluginJars(fileTree("plugins"))
        pluginJars(project(":plugin:paper").tasks.shadowJar.flatMap {
            it.archiveFile
        })
        pluginJars(project(":test-plugin").tasks.jar.flatMap {
            it.archiveFile
        })
        version(minecraft)
        downloadPlugins {
            hangar("ViaVersion", "5.4.2")
            hangar("ViaBackwards", "5.4.2")
            hangar("Skript", "2.12.2")
        }
    }
    build {
        finalizedBy(
            javadocJar
        )
    }
    shadowJar {
        enabled = false
    }
}

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
                jar = project(":plugin:paper").tasks.shadowJar.flatMap {
                    it.archiveFile
                }
                platformVersions = SUPPORTED_VERSIONS
                dependencies {
                    hangar("SkinsRestorer") { required = false }
                }
            }
        }
    }
}