import magik.createGithubPublication
import magik.github
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.lwjgl.lwjgl
import org.lwjgl.Lwjgl.Module.*

plugins {
    kotlin("jvm")
    id("org.lwjgl.plugin")
    id("elect86.magik")
    `maven-publish`
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(projects.core)
    implementation(projects.glfw)

    api("kotlin.graphics:uno:0.7.21")
    lwjgl { implementation(jemalloc, glfw, opengl, remotery, stb) }

    testImplementation("com.github.ajalt:mordant:1.2.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
}

kotlin.jvmToolchain { languageVersion.set(JavaLanguageVersion.of(8)) }

tasks {
    withType<KotlinCompilationTask<*>>().configureEach { compilerOptions.freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package") }
    test { useJUnitPlatform() }
}

publishing {
    publications {
        createGithubPublication {
            from(components["java"])
            artifactId = "${rootProject.name}-${project.name}"
            suppressAllPomMetadataWarnings()
        }
    }
    repositories.github { domain = "kotlin-graphics/mary" }
}

java.withSourcesJar()