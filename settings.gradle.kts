plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "BetterModel"

include(
    "api",
    "core",
//    "nms:v1_19_R3",
//    "nms:v1_20_R1",
    "nms:v1_20_R2",
    "nms:v1_20_R3",
    "nms:v1_20_R4",
    "nms:v1_21_R1",
    "nms:v1_21_R2",
    "nms:v1_21_R3"
)