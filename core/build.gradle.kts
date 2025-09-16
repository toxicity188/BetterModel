import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.bukkitConvention)
    alias(libs.plugins.paperweight) apply false
    alias(libs.plugins.resourceFactoryBukkit)
}

repositories {
    maven("https://maven.citizensnpcs.co/repo/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.hibiscusmc.com/releases")
    maven("https://repo.nexomc.com/releases/")
}

dependencies {
    shade(project(":api"))
    shade(project(":purpur"))
    rootProject.project("nms").subprojects.forEach {
        shade(project(":nms:${it.name}", configuration = "reobf"))
    }
    shade(libs.bundles.shadedLibrary) {
        exclude("net.kyori")
        exclude("org.ow2.asm")
    }

    compileOnly("net.citizensnpcs:citizens-main:2.0.39-SNAPSHOT") {
        exclude("net.byteflux")
    }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.7.10")
    compileOnly("io.lumine:Mythic-Dist:5.9.5")
    compileOnly("com.hibiscusmc:HMCCosmetics:2.8.2-0676125f")
    compileOnly("com.nexomc:nexo:1.11.0-dev")
    shade(fileTree("libs"))
}

bukkitPluginYaml {
    main = "$group.BetterModelPluginImpl"
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
        "HMCCosmetics",
        "SkinsRestorer",
        "Nexo"
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