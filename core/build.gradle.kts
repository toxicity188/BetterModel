import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.bukkitConvention)
    alias(libs.plugins.paperweight) apply false
    alias(libs.plugins.resourceFactoryBukkit)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":purpur"))
    rootProject.project("nms").subprojects.forEach {
        implementation(project(":nms:${it.name}", configuration = "reobf"))
    }
    implementation(libs.bundles.shadedLibrary)
}

bukkitPluginYaml {
    main = "$group.BetterModelPluginImpl"
    version = project.version.toString()
    name = rootProject.name
    foliaSupported = true
    apiVersion = "1.20"
    author = "toxicity"
    description = "Lightweight BlockBench model engine & entity animation"
    softDepend = listOf(
        "MythicMobs",
        "Citizens",
        "HMCCosmetics"
    )
    libraries = libs.bundles.library.map {
        it.map { lib ->
            lib.toString()
        }
    }
    permissions.create("bettermodel") {
        default = Permission.Default.OP
        description = "Accesses to command."
        children = mapOf(
            "reload" to true,
            "spawn" to true,
            "disguise" to true,
            "undisguise" to true,
            "play" to true,
            "limb" to true
        )
    }
}