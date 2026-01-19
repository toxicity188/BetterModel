plugins {
    alias(libs.plugins.convention.standard)
}

dependencies {
    compileOnly(libs.bundles.minecraft)
    compileOnly("com.mojang:authlib:7.0.61")

    compileOnly(project(":api"))

    compileOnly(libs.bundles.core)
    compileOnly(libs.cloud.core)
}
