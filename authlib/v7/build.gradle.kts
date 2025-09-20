plugins {
    alias(libs.plugins.paperweightConvention)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:7.0.60")
}