import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import kotlin.io.encoding.Base64

plugins {
    alias(libs.plugins.convention.bukkit)
    id("com.vanniktech.maven.publish") version "0.35.0"
    signing
}

val artifactBaseId = rootProject.name.lowercase()
val artifactVersion = project.version.toString().substringBeforeLast('-')

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    val key = System.getenv("SIGNING_KEY")?.let {
        Base64.decode(it.toByteArray()).toString(Charsets.UTF_8)
    }
    val password = System.getenv("SIGNING_PASSWORD")
    if (!key.isNullOrEmpty() && !password.isNullOrEmpty()) {
        useInMemoryPgpKeys(
            key,
            password
        )
    } else useGpgCmd()
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.toxicity188", artifactBaseId, artifactVersion)
    configure(JavaLibrary(
        javadocJar = JavadocJar.None(),
        sourcesJar = true,
    ))
    pom {
        name = artifactBaseId
        description = "Modern Bedrock model engine for Bukkit"
        inceptionYear = "2024"
        url = "https://github.com/toxicity188/BetterModel/"
        licenses {
            license {
                name = "MIT License"
                url = "https://mit-license.org/"
            }
        }
        developers {
            developer {
                id = "toxicity188"
                name = "toxicity188"
                url = "https://github.com/toxicity188/"
            }
        }
        scm {
            url = "https://github.com/toxicity188/BetterModel/"
            connection = "scm:git:git://github.com/toxicity188/BetterModel.git"
            developerConnection = "scm:git:ssh://git@github.com/toxicity188/BetterModel.git"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/toxicity188/$artifactBaseId")
            credentials {
                username = "toxicity188"
                password = System.getenv("PACKAGES_API_TOKEN")
            }
        }
    }
}