plugins {
    id("com.modrinth.minotaur")
}

val versionString = version.toString()
val classifier = project.name
    .substringAfterLast('-')
    .replaceFirstChar { it.uppercase() }

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
    additionalFiles {
        javadocJar(rootProject.layout.buildDirectory.file("libs/${rootProject.name}-$versionString-javadoc.jar"))
    }
    versionNumber = versionString
    versionName = "BetterModel $versionString for $classifier"
}
