plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.build.kotlin.jvm)
    implementation(libs.build.shadow)
    implementation(libs.build.hangarPublish)
    implementation(libs.build.minotaur)

    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    implementation("com.github.hierynomus.license:com.github.hierynomus.license.gradle.plugin:0.16.1")
}