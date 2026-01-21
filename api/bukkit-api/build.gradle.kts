plugins {
    alias(libs.plugins.convention.publish)
    alias(libs.plugins.convention.bukkit)
}

dependencies {
    compileOnly(project(":api"))
}
