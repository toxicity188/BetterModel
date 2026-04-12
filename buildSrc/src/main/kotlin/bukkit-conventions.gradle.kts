plugins {
    id("standard-conventions")
}

val minecraft = property("minecraft_version").toString()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraft.build.+")
    testImplementation("io.papermc.paper:paper-api:$minecraft.build.+")
}
