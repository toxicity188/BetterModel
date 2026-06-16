plugins {
    alias(libs.plugins.convention.standard)
}

dependencies {
    compileOnly(project(":bettermodel-api"))
    compileOnly(project(":bettermodel-api:bettermodel-bukkit-api"))
    //TODO compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}.build.+")
    compileOnly("org.purpurmc.purpur:purpur-api:26.1.2.build.+")
}
