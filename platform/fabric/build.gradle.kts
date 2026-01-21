plugins {
    alias(libs.plugins.convention.publish)
    alias(libs.plugins.convention.modrinth)
    alias(libs.plugins.resourcefactory.fabric)
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
    // Access winder
    accessWidenerPath = file("src/main/resources/bettermodel.accesswidener")

    // Run
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

    // Test mod
    createRemapConfigurations(sourceSets["testmod"])
}


dependencies {
    // Minecraft
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("io.papermc.parchment.data:parchment:${property("parchment_version")}")
    })

    api(project(":api")); include(project(":api"))
    api(project(":core")); include(project(":core"))

    setOf(
        "fabric-api-base",
        "fabric-command-api-v2",
        "fabric-data-attachment-api-v1",
        "fabric-entity-events-v1",
        "fabric-events-interaction-v0",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-transitive-access-wideners-v1"
    ).forEach {
        modImplementation(fabricApi.module(it, libs.versions.fabric.api.get()))
    }

    modImplementation(libs.bundles.fabric)

    implementation(libs.bundles.fabric.library); include(libs.bundles.fabric.library)
    modApi(libs.bundles.fabric.mod); include(libs.bundles.fabric.mod)

    implementation(libs.bundles.core); include(libs.bundles.core)
    include(libs.bundles.library)

}

fabricModJson {
    id = "bettermodel"
    name = rootProject.name
    description = "Modern Bedrock model engine for Minecraft Java Edition"

    entrypoints = listOf(
        mainEntrypoint(
            "$group.impl.fabric.BetterModelFabric"
        )
    )

    depends = mapOf(
        "minecraft" to listOf("~${property("minecraft_version")}"),
        "fabricloader" to listOf("*"),

        // fabric-api
        "fabric-api-base" to listOf("*"),
        "fabric-command-api-v2" to listOf("*"),
        "fabric-data-attachment-api-v1" to listOf("*"),
        "fabric-entity-events-v1" to listOf("*"),
        "fabric-events-interaction-v0" to listOf("*"),
        "fabric-lifecycle-events-v1" to listOf("*"),
        "fabric-networking-api-v1" to listOf("*"),
        "fabric-transitive-access-wideners-v1" to listOf("*"),

        // mod libraries
        "adventure-platform-fabric" to listOf("*"),
        "cloud" to listOf("*"),
        "polymer-resource-pack" to listOf("*")
    )

    mixins = listOf(
        mixin("bettermodel.mixins.json")
    )

    authors = listOf(
        person("toxicity188")
    )
    contributors = listOf(
        person("Kouvali (Fabric Port)")
    )
    contact {
        sources = "https://github.com/toxicity188/BetterModel/"
        issues = "https://github.com/toxicity188/BetterModel/issues"
    }
    icon("assets/icon.png")
    mitLicense()

    version = project.version.toString()
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

tasks {
    jar {
        from(rootProject.layout.projectDirectory.file("LICENSE.md"))
        from(rootProject.layout.projectDirectory.file(".idea/icon.png")) {
            rename { "assets/icon.png" }
        }
    }
    remapJar {
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
    runServer {
        enabled = false
    }
}

modrinth {
    loaders = listOf("fabric", "quilt")
    uploadFile.set(tasks.remapJar)
    gameVersions = listOf("1.21.11")
    dependencies {
        required.project(
            "fabric-api",
            "fabric-language-kotlin"
        )
//        optional.project(
//            "skinsrestorer"
//        )
    }
}
