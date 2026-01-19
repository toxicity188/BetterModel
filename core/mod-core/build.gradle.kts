plugins {
    alias(libs.plugins.convention.mod)
    alias(libs.plugins.resourcefactory.fabric)
    id("fabric-loom")
}

loom {
    accessWidenerPath = file("src/main/resources/bettermodel.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("io.papermc.parchment.data:parchment:${property("parchment_version")}")
    })
    modImplementation(libs.bundles.fabric)

    api(project(":core"))?.let { include(it) }
    api(project(":api:mod-api", "namedElements")); include(project(":api:mod-api"))

    api(libs.bundles.fabric.library)?.let { include(it) }
    modApi(libs.bundles.fabric.mod)?.let { include(it) }

    api(libs.bundles.core)?.let { include(it) }
    api(libs.bundles.library)?.let { include(it) }
}

fabricModJson {
    id = "bettermodel-core"
    version = project.version.toString()

    entrypoints = listOf(
        mainEntrypoint(
            "$group.fabric.BetterModelFabricImpl"
        )
    )

    depends = mapOf(
        "minecraft" to listOf("~${property("minecraft_version")}"),
        "fabricloader" to listOf("*"),
        "fabric-api" to listOf("*"),

        // mod modules
        "bettermodel-api" to listOf("*"),

        // mod libraries
        "adventure-platform-fabric" to listOf("*"),
        "cloud" to listOf("*"),
        "polymer-autohost" to listOf("*"),
        "polymer-resource-pack" to listOf("*")
    )
    mixins = listOf(
        mixin("bettermodel.mixins.json")
    )
}
