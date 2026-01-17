plugins {
    id("com.modrinth.minotaur")
}

val versionString = version.toString()
val classifier = project.name

modrinth {
    token = System.getenv("MODRINTH_API_TOKEN")
    projectId = "bettermodel"
    syncBodyFrom = rootProject.file("BANNER.md").readText()
    val log = System.getenv("COMMIT_MESSAGE")
    if (log != null) {
        versionType = "beta"
        changelog = log
    } else {
        versionType = "release"
        changelog = rootProject.file("changelog/$versionString.md").readText()
    }
    additionalFiles = listOf(
        rootProject.layout.buildDirectory.file("libs/${rootProject.name}-$versionString-javadoc.jar")
    )
    versionNumber = versionString
    versionName = "BetterModel $versionString for ${classifier.replaceFirstChar { it.uppercase() }}"
//    uploadFile.set(tasks.shadowJar)
//    gameVersions = SUPPORTED_VERSIONS
//    dependencies {
//        optional.project(
//            "mythicmobs",
//            "skinsrestorer"
//        )
//    }
}
