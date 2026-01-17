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
    api(project(":api:mod-api", "namedElements"))
    include(project(":api:mod-api"))

    // non-mod modules
    api(project(":core"))?.let { include(it) }

    // mod libraries
    modApi(libs.adventure.platform.fabric)
    modApi(libs.polymer.resource.pack)
    modApi(libs.cloud.fabric)

    // non-mod libraries
    implementation(libs.configurate.yaml)

    // include
    include(libs.bundles.library)
    include(libs.bundles.core)
    include(libs.bundles.fabric.include)
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
        "polymer-resource-pack" to listOf("*")
    )
    mixins = listOf(
        mixin("bettermodel.mixins.json")
    )
}
