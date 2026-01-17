plugins {
    alias(libs.plugins.convention.mod)
    alias(libs.plugins.resourcefactory.fabric)
    id("fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("io.papermc.parchment.data:parchment:${property("parchment_version")}")
    })
    modImplementation(libs.bundles.fabric)

    // non-mod modules
    api(project(":api"))?.let { include(it) }
}

fabricModJson {
    id = "bettermodel-api"
    version = project.version.toString()

    depends = mapOf(
        "minecraft" to listOf("~${property("minecraft_version")}"),
        "fabricloader" to listOf("*"),
        "fabric-api" to listOf("*")
    )
}
