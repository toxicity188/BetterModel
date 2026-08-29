plugins {
    alias(libs.plugins.convention.standard)
}

dependencies {
    compileOnly(project(":bettermodel-api"))
    compileOnly(project(":bettermodel-api:bettermodel-bukkit-api"))
    compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}.build.+")
}
