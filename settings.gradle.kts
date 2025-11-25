plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "BetterModel"

include(
    "api",
    "core",
    "purpur",
    "plugin:spigot",
    "plugin:paper",
    "authlib:v6",
    "authlib:v7",
    "nms:v1_20_R4",
    "nms:v1_21_R1",
    "nms:v1_21_R2",
    "nms:v1_21_R3",
    "nms:v1_21_R4",
    "nms:v1_21_R5",
    "nms:v1_21_R6",
    //"nms:v1_21_R7",
    "test-plugin"
)