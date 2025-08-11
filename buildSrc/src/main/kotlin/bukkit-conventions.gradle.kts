plugins {
    id("standard-conventions")
}

val minecraft = property("minecraft_version").toString()

rootProject.dependencies.dokka(project)

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraft-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:$minecraft-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:6.0.58")
}