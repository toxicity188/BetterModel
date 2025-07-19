plugins {
    alias(libs.plugins.paperweightConvention)
}

repositories {
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
}