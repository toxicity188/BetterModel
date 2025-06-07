import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish") version "0.32.0"
    signing
}

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    useGpgCmd()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
}

mavenPublishing  {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates("io.github.toxicity188", rootProject.name, project.version as String)
    configure(JavaLibrary(
        javadocJar = JavadocJar.None(),
        sourcesJar = true,
    ))
    pom {
        name = rootProject.name
        description = "Lightweight BlockBench model engine & entity animation"
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