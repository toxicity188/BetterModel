plugins {
    alias(libs.plugins.convention.api)
    alias(libs.plugins.convention.bukkit)
}

dependencies {
    compileOnly(project(":api"))
}
