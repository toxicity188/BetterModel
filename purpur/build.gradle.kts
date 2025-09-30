plugins {
    alias(libs.plugins.convention.paperweight)
}

repositories {
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    //compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
    compileOnly("org.purpurmc.purpur:purpur-api:1.21.8-R0.1-SNAPSHOT")
}