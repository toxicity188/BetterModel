plugins {
    id("standard-conventions")
}

val minecraft = "1.21.11" //TODO change this to property("minecraft_version").toString()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraft-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:$minecraft-R0.1-SNAPSHOT")
}
