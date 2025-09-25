plugins {
    alias(libs.plugins.convention.plugin)
}

dependencies {
    shade(project(":nms:v1_20_R4", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R1", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R2", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R3", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R4", configuration = "reobf")) { isTransitive = false }
    shade(project(":nms:v1_21_R5", configuration = "reobf")) { isTransitive = false }
    //shade(project(":nms:v1_21_R6", configuration = "reobf")) { isTransitive = false }
    shade(libs.commandapi.bukkit)
}

modrinth {
    loaders = BUKKIT_LOADERS + PAPER_LOADERS
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "spigot"
    }
}
