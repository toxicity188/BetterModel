pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("net.fabricmc.fabric-loom-repositories") version "1.16-SNAPSHOT"
    id("net.neoforged.moddev.repositories") version "2.0.141"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://repo.alessiodp.com/releases/")
        maven("https://maven.blamejared.com/")
        maven("https://repo.purpurmc.org/snapshots")
        maven("https://maven.citizensnpcs.co/repo/")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://maven.nucleoid.xyz/")
        maven("https://repo.nexomc.com/releases/")
        // for development builds
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            name = "central-snapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}

rootProject.name = "bettermodel"

val published = setOf(
    "api",
    "api:bukkit-api",
    "api:mod-api",

    "core",
    "core:bukkit-core",

    "platform:spigot",
    "platform:paper",
    "platform:fabric",
)

include(published)
include(
    "purpur",

    //nms
    "nms:v1_21_R3",
    "nms:v1_21_R4",
    "nms:v1_21_R5",
    "nms:v1_21_R6",
    "nms:v1_21_R7",
    "nms:v26_R1",
    "nms:v26_R2",

    //test
    "test-plugin"
)

published.forEach { target ->
    findProject(":$target")?.let {
        it.name = "${rootProject.name}-${it.name}"
    }
}
