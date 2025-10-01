import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.convention.bukkit)
    alias(libs.plugins.paperweight) apply false
    alias(libs.plugins.resourcefactory.bukkit)
}

repositories {
    maven("https://maven.citizensnpcs.co/repo/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases/")
}

dependencies {
    shade(project(":api"))
    shade(project(":purpur"))
    rootProject.project("nms").subprojects.forEach {
        compileOnly(it)
    }
    rootProject.project("authlib").subprojects.forEach {
        shade(it)
    }
    shade(libs.bundles.shadedLibrary) {
        exclude("net.kyori")
        exclude("org.ow2.asm")
    }
    compileOnly(libs.commandapi.bukkit)

    compileOnly(libs.bundles.manifestLibrary)
    testImplementation(libs.bundles.manifestLibrary)

    compileOnly("net.citizensnpcs:citizens-main:2.0.39-SNAPSHOT") {
        exclude("net.byteflux")
    }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.8.0")
    compileOnly("io.lumine:Mythic-Dist:5.10.1")
    compileOnly("com.nexomc:nexo:1.12.1-dev")
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