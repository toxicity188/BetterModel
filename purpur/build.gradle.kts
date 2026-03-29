plugins {
    alias(libs.plugins.convention.paperweight)
}

dependencies {
    //TODO change this to compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
    compileOnly("org.purpurmc.purpur:purpur-api:1.21.11-R0.1-SNAPSHOT")
}
