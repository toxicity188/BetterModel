plugins {
    alias(libs.plugins.convention.plugin)
}

dependencies {
    shade(project(":nms:v1_20_R4")) { isTransitive = false }
    shade(project(":nms:v1_21_R1")) { isTransitive = false }
    shade(project(":nms:v1_21_R2")) { isTransitive = false }
    shade(project(":nms:v1_21_R3")) { isTransitive = false }
    shade(project(":nms:v1_21_R4")) { isTransitive = false }
    shade(project(":nms:v1_21_R5")) { isTransitive = false }
    shade(project(":nms:v1_21_R6")) { isTransitive = false }
    shade(libs.commandapi.paper)
}

modrinth {
    loaders = PAPER_LOADERS
}

tasks.modrinth {
    dependsOn(tasks.modrinthSyncBody)
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}