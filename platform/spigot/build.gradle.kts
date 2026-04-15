import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.convention.plugin)
    alias(libs.plugins.resourcefactory.bukkit)
}

val dependenciesContent: List<String> = libs.bundles.library.map {
    it.map(Any::toString)
}.get()

dependencies {
    shade(project(":nms:bettermodel-nms-v1_21_R3", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:bettermodel-nms-v1_21_R4", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:bettermodel-nms-v1_21_R5", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:bettermodel-nms-v1_21_R6", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:bettermodel-nms-v1_21_R7", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:bettermodel-nms-v26_R1")) { isTransitive = false }
}

modrinth {
    gameVersions = SUPPORTED_VERSIONS
    loaders = BUKKIT_LOADERS
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "spigot"
    }
}

bukkitPluginYaml {
    main = "$group.spigot.BetterModelSpigot"
    version = project.version.toString()
    name = "BetterModel"
    foliaSupported = true
    apiVersion = "1.21.4"
    author = "toxicity188"
    description = "Modern Bedrock model engine for Minecraft Java Edition"
    website = "https://modrinth.com/plugin/bettermodel"
    softDepend = listOf(
        "MythicMobs",
        "Citizens",
        "SkinsRestorer"
    )
    libraries = dependenciesContent
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
