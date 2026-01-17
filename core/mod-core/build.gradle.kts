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

    // mod modules
    api(project(":api:mod-api", "namedElements"))?.let { include(it) }

    // non-mod modules
    api(project(":core"))?.let { include(it) }

    // mod libraries
    modApi(libs.adventure.platformFabric)
    modApi(libs.polymer.resource.pack)
    modApi(libs.cloud.fabric)

    // non-mod libraries
    implementation(libs.jackson.dataformat.yaml)?.let { include(it) }
}

fabricModJson {
    id = "bettermodel-core"
    version = project.version.toString()

    entrypoints = listOf(
        serverEntrypoint(
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
        "polymer-resource-pack" to listOf("*")
    )
}
