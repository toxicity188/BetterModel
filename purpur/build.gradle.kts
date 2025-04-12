repositories {
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:${property("minecraft_version")}-R0.1-SNAPSHOT")
}