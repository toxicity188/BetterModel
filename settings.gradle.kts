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
    id("fabric-loom") version "1.15-SNAPSHOT"
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
        maven("https://repo.nexomc.com/releases/")
        maven("https://maven.nucleoid.xyz/")
    }
}

rootProject.name = "BetterModel"

include(
    //api
    "api",
    "api:bukkit-api",

    //core
    "core",
    "core:bukkit-core",

    "purpur",

    "platform:spigot",
    "platform:paper",
    "platform:fabric",

    //nms
    "nms:v1_21_R1",
    "nms:v1_21_R3",
    "nms:v1_21_R4",
    "nms:v1_21_R5",
    "nms:v1_21_R6",
    "nms:v1_21_R7",

    //test
    "test-plugin"
)
