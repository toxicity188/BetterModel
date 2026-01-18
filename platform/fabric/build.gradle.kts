plugins {
    alias(libs.plugins.convention.mod)
    alias(libs.plugins.resourcefactory.fabric)
    alias(libs.plugins.convention.modrinth)
    id("fabric-loom")
}

val versionString = "${rootProject.version}+${property("minecraft_version")}"
val classifier = project.name

project.version = versionString

sourceSets {
    create("testmod") {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

loom {
    runs {
        create("testClient") {
            client()
            configName = "Test Minecraft Client"
            source("testmod")
        }

        create("testServer") {
            server()
            configName = "Test Minecraft Server"
            source("testmod")
        }
    }

    createRemapConfigurations(sourceSets["testmod"])
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("io.papermc.parchment.data:parchment:${property("parchment_version")}")
    })
    modImplementation(libs.bundles.fabric)

    // mod modules
    api(project(":core:mod-core", "namedElements"))
    include(project(":core:mod-core"))
}

fabricModJson {
    id = "bettermodel"
    version = project.version.toString()

    depends = mapOf(
        "minecraft" to listOf("~${property("minecraft_version")}"),
        "fabricloader" to listOf("*"),
        "fabric-api" to listOf("*"),

        // mod modules
        "bettermodel-core" to listOf("*")
    )
}

sourceSets["testmod"].resourceFactory {
    fabricModJson {
        id = "bettermodel-testmod"
        version = project.version.toString()

        entrypoints = listOf(
            mainEntrypoint(
                "$group.test.RollTest"
            )
        )

        depends = mapOf(
            // mod modules
            "bettermodel" to listOf("*")
        )
    }
}

tasks.remapJar {
    manifest {
        attributes(
            mapOf(
                "Dev-Build" to (BUILD_NUMBER ?: -1),
                "Version" to versionString,
                "Author" to "toxicity188",
                "Url" to "https://github.com/toxicity188/BetterModel",
                "Created-By" to "Gradle $gradle",
                "Build-Jdk" to "${System.getProperty("java.vendor")} ${System.getProperty("java.version")}",
                "Build-OS" to "${System.getProperty("os.arch")} ${System.getProperty("os.name")}"
            )
        )
    }
    archiveBaseName = rootProject.name
    archiveClassifier = classifier
    destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
}

modrinth {
    loaders = listOf("fabric", "quilt")
    uploadFile.set(tasks.remapJar)
    gameVersions = SUPPORTED_VERSIONS
    dependencies {
        required.project(
            "fabric-api"
        )
        optional.project(
            "skinsrestorer"
        )
    }
}
