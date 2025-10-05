import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    alias(libs.plugins.convention.plugin)
    alias(libs.plugins.resourcefactory.paper)
}

val libraryDir = layout.buildDirectory.file("generated/paper-library")

dependencies {
    shade(project(":nms:v1_20_R4")) { isTransitive = false }
    shade(project(":nms:v1_21_R1")) { isTransitive = false }
    shade(project(":nms:v1_21_R2")) { isTransitive = false }
    shade(project(":nms:v1_21_R3")) { isTransitive = false }
    shade(project(":nms:v1_21_R4")) { isTransitive = false }
    shade(project(":nms:v1_21_R5")) { isTransitive = false }
    shade(project(":nms:v1_21_R6")) { isTransitive = false }
    shade(libs.commandapi.paper)
}

modrinth {
    loaders = PAPER_LOADERS
}

tasks.modrinth {
    dependsOn(tasks.modrinthSyncBody)
}

val generatePaperLibrary by tasks.registering {
    outputs.file(libraryDir)
    doLast {
        val text = libs.bundles.library.get().joinToString("\n") { dep -> dep.toString() }
        val file = libraryDir.get().asFile
        file.parentFile.mkdirs()
        file.writeText(text)
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
    main = "$group.paper.BetterModelPluginPaper"
    loader = "$group.paper.BetterModelLoader"
    version = project.version.toString()
    name = rootProject.name
    foliaSupported = true
    apiVersion = "1.20"
    author = "toxicity188"
    description = "Modern Bedrock model engine for Bukkit"
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
            "version" to true
        )
    }
}