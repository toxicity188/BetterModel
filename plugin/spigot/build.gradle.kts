import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.convention.plugin)
    alias(libs.plugins.resourcefactory.bukkit)
}

dependencies {
    shade(project(":nms:v1_20_R4", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R1", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R2", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R3", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R4", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R5", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R6", configuration = "reobf")) { isTransitive = false }
    compileOnly(libs.commandapi.spigot)
}

modrinth {
    loaders = BUKKIT_LOADERS
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "spigot"
    }
}

bukkitPluginYaml {
    main = "$group.spigot.BetterModelPluginSpigot"
    version = project.version.toString()
    name = rootProject.name
    foliaSupported = true
    apiVersion = "1.20"
    author = "toxicity188"
    description = "Modern Bedrock model engine for Bukkit"
    website = "https://modrinth.com/plugin/bettermodel"
    softDepend = listOf(
        "MythicMobs",
        "Citizens",
        "SkinsRestorer"
    )
    libraries = libs.bundles.library.map {
        it.map(Any::toString)
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