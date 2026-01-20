plugins {
    alias(libs.plugins.convention.mod)
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

    api(project(":core")); include(project(":core"))
    api(project(":api:mod-api", "namedElements")); include(project(":api:mod-api"))

    api(libs.bundles.fabric.library); include(libs.bundles.fabric.library)
    modApi(libs.bundles.fabric.mod); include(libs.bundles.fabric.mod)

    api(libs.bundles.core); include(libs.bundles.core)
    api(libs.bundles.library); include(libs.bundles.library)
}

fabricModJson {
    id = "bettermodel-core"
    name = "${rootProject.name}-core"
    description = "BetterModel's mod core module."

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

    version = project.version.toString()
}

tasks.runServer {
    enabled = false
}
