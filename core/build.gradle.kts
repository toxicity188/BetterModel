plugins {
    alias(libs.plugins.convention.publish)
}

dependencies {
    api(project(":api"))

    compileOnly(libs.bundles.minecraft)
    compileOnly("com.mojang:authlib:7.0.61")

    compileOnly(libs.bundles.core)
    compileOnly(libs.cloud.core)
}
