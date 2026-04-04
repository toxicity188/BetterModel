import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    alias(libs.plugins.convention.plugin)
    alias(libs.plugins.resourcefactory.paper)
}

val libraryDir: Provider<RegularFile> = layout.buildDirectory.file("generated/paper-library")
val dependenciesContent: String = libs.bundles.library.map { bundle ->
    bundle.joinToString("\n") { dep -> dep.toString() }
}.get()

dependencies {
    shade(project(":nms:v1_21_R1")) { isTransitive = false }
    shade(project(":nms:v1_21_R3")) { isTransitive = false }
    shade(project(":nms:v1_21_R4")) { isTransitive = false }
    shade(project(":nms:v1_21_R5")) { isTransitive = false }
    shade(project(":nms:v1_21_R6")) { isTransitive = false }
    shade(project(":nms:v1_21_R7")) { isTransitive = false }
    shade(project(":nms:v26_R1")) { isTransitive = false }
}

modrinth {
    gameVersions = SUPPORTED_VERSIONS
    loaders = PAPER_LOADERS
}

tasks.modrinth {
    dependsOn(tasks.modrinthSyncBody)
}

val generatePaperLibrary by tasks.registering {
    val outputProvider = libraryDir
    val contentProvider = dependenciesContent

    outputs.file(outputProvider)

    doLast {
        val file = outputProvider.get().asFile
        file.parentFile.mkdirs()
        file.writeText(contentProvider)
    }
}

tasks.shadowJar {
    dependsOn(generatePaperLibrary)
    from(libraryDir)
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

paperPluginYaml {
    main = "$group.paper.BetterModelPaper"
    loader = "$group.paper.BetterModelLoader"
    version = project.version.toString()
    name = "BetterModel"
    foliaSupported = true
    apiVersion = "1.20"
    author = "toxicity188"
    contributors = listOf("https://github.com/toxicity188/BetterModel/graphs/contributors")
    description = "Modern Bedrock model engine for Minecraft Java Edition"
    website = "https://modrinth.com/plugin/bettermodel"
    dependencies {
        server(
            name = "MythicMobs",
            required = false,
            load = PaperPluginYaml.Load.BEFORE
        )
        server(
            name = "Citizens",
            required = false,
            load = PaperPluginYaml.Load.BEFORE
        )
        server(
            name = "SkinsRestorer",
            required = false,
            load = PaperPluginYaml.Load.BEFORE
        )
        server(
            name = "Nexo",
            required = false,
            load = PaperPluginYaml.Load.OMIT
        )
    }
    permissions.create("bettermodel") {
        default = Permission.Default.OP
        description = "Accesses to command."
        children = mapOf(
            "reload" to true,
            "spawn" to true,
            "disguise" to true,
            "undisguise" to true,
            "test" to true,
            "play" to true,
            "version" to true,
            "hide" to true,
            "show" to true
        )
    }
}
