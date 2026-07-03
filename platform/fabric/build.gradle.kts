import xyz.jpenilla.resourcefactory.fabric.Environment

plugins {
    alias(libs.plugins.convention.publish)
    alias(libs.plugins.convention.modrinth)
    alias(libs.plugins.resourcefactory.fabric)
    id("net.fabricmc.fabric-loom")
}

val versionString = "${rootProject.version}+${property("minecraft_version")}"

val jarName = "${rootProject.name}-$versionString-${project.name.substringAfterLast('-')}.jar"
val jarDir = rootProject.layout.buildDirectory.dir("libs")

sourceSets {
    create("testmod") {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

loom {
    // Access winder
    //accessWidenerPath = file("src/main/resources/bettermodel.accesswidener")

    // Run
    runs {
        create("testClient") {
            client()
            displayName = "Test Minecraft Client"
            sourceSet = "testmod"
        }

        create("testServer") {
            server()
            displayName = "Test Minecraft Server"
            sourceSet = "testmod"
        }
    }

    // Test mod
    //createRemapConfigurations(sourceSets["testmod"])
}

dependencies {
    // Minecraft
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    api(project(":bettermodel-api")); include(project(":bettermodel-api"))
    api(project(":bettermodel-api:bettermodel-mod-api")); include(project(":bettermodel-api:bettermodel-mod-api"))
    api(project(":bettermodel-core")); include(project(":bettermodel-core"))

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
        implementation(fabricApi.module(it, libs.versions.fabric.api.get()))
    }

    implementation(libs.bundles.fabric)

    implementation(libs.bundles.fabric.library); include(libs.bundles.fabric.library)
    api(libs.bundles.fabric.mod); include(libs.bundles.fabric.mod)

    implementation(libs.bundles.core); include(libs.bundles.core)
    implementation(libs.concurrentutil); include(libs.concurrentutil)
    include(libs.bundles.library)
}

fabricModJson {
    id = "bettermodel"
    name = "BetterModel"
    description = "Modern Bedrock model engine for Minecraft Java Edition"

    entrypoints = listOf(
        mainEntrypoint("$group.impl.fabric.BetterModelFabricImpl") {
            adapter = "kotlin"
        }
    )

    environment = Environment.ANY

    depends = mapOf(
        "minecraft" to listOf(">=${LATEST_VERSION.first()}"),
        "fabricloader" to listOf("*"),
        "fabric-language-kotlin" to listOf(">=${libs.versions.fabric.language.kotlin.get()}"),

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
        homepage = "https://modrinth.com/plugin/bettermodel"
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

interface FsInjected {
    @get:Inject val fs: FileSystemOperations
}
val copyModJar = tasks.register("copyModJar") {
    description = "Copies mod jar to build/libs."
    val injected = objects.newInstance<FsInjected>()
    val archiveFile = tasks.jar.flatMap { it.archiveFile }
    val jarName = jarName
    val jarDir = jarDir
    doLast {
        injected.fs.copy {
            from(archiveFile)
            rename { jarName }
            into(jarDir)
        }
    }
}

tasks {
    jar {
        from(rootProject.layout.projectDirectory.file("LICENSE.md"))
        from(rootProject.layout.projectDirectory.file(".idea/icon.png")) {
            rename { "assets/icon.png" }
        }
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
        finalizedBy(copyModJar)
    }
    runServer {
        enabled = false
    }
}

modrinth {
    loaders = listOf("fabric", "quilt")
    uploadFile.set(jarDir.map { it.file(jarName) })
    gameVersions = LATEST_VERSION
    dependencies {
        required.version("fabric-api", libs.versions.fabric.api.get())
        required.version("fabric-language-kotlin", libs.versions.fabric.language.kotlin.get())
//        optional.project(
//            "skinsrestorer"
//        )
    }
}
