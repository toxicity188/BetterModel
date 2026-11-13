plugins {
    id("standard-conventions")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(project(":bettermodel-api"))
    compileOnly(project(":bettermodel-api:bettermodel-bukkit-api"))
}
