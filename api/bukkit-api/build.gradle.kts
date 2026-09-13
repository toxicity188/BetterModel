plugins {
    alias(libs.plugins.convention.publish)
    alias(libs.plugins.convention.bukkit)
}

dependencies {
    api(project(":bettermodel-api"))
}
