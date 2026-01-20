plugins {
    alias(libs.plugins.convention.api)
    alias(libs.plugins.convention.mod)
    id("fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("io.papermc.parchment.data:parchment:${property("parchment_version")}")
    })
    modImplementation(libs.bundles.fabric)

    api(project(":api"))?.let { include(it) }
}

fabricModJson {
    id = "bettermodel-api"
    name = "${rootProject.name}-api"
    description = "BetterModel's mod API module."

    depends = mapOf(
        "minecraft" to listOf("~${property("minecraft_version")}"),
        "fabricloader" to listOf("*"),
        "fabric-api" to listOf("*")
    )

    version = project.version.toString()
}

tasks.runServer {
    enabled = false
}
