plugins {
    id("standard-conventions")
}

dependencies {
    compileOnly(project(":bettermodel-api"))
    compileOnly(project(":bettermodel-api:bettermodel-bukkit-api"))
}
