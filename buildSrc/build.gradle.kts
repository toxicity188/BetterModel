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
    implementation(libs.build.resourcefactory)
    implementation(libs.build.paperweight)

    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
    implementation("dev.yumi.gradle.licenser:dev.yumi.gradle.licenser.gradle.plugin:4.0.0")
    implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.37.0")
}
