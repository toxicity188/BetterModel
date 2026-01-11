plugins {
    alias(libs.plugins.convention.paperweight)
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
}
