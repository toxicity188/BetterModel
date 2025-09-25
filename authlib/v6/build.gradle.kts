plugins {
    alias(libs.plugins.convention.paperweight)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:6.0.58")
}