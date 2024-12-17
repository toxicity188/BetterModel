plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ModelRenderer"

include(
    "api",
    "core",
    "nms:v1_21_R2",
    "nms:v1_21_R3"
)