plugins {
    alias(libs.plugins.convention.mod)
    alias(libs.plugins.resourcefactory.fabric)
    id("fabric-loom")
}

sourceSets {
    create("testmod") {
        compileClasspath += main.get().compileClasspath + main.get().output
        runtimeClasspath += main.get().runtimeClasspath + main.get().output
    }
}

loom {
    runs {
        create("testClient") {
            client()
            configName = "Test Minecraft Client"
            source("testmod")
        }

        create("testServer") {
            server()
            configName = "Test Minecraft Server"
            source("testmod")
        }
    }

    createRemapConfigurations(sourceSets["testmod"])
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("io.papermc.parchment.data:parchment:${property("parchment_version")}")
    })
    modImplementation(libs.bundles.fabric)

    // mod modules
    api(project(":core:mod-core", "namedElements"))?.let { include(it) }
}

fabricModJson {
    id = "bettermodel"
    version = project.version.toString()

    depends = mapOf(
        "minecraft" to listOf("~${property("minecraft_version")}"),
        "fabricloader" to listOf("*"),
        "fabric-api" to listOf("*"),

        // mod modules
        "bettermodel-core" to listOf("*")
    )
}

sourceSets["testmod"].resourceFactory {
    fabricModJson {
        id = "bettermodel-testmod"
        version = project.version.toString()

        depends = mapOf(
            // mod modules
            "bettermodel" to listOf("*")
        )
    }
}
