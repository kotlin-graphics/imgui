import magik.createGithubPublication
import magik.github
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm") version "1.8.20"
    id("org.lwjgl.plugin") version "0.0.34"
    id("elect86.magik") version "0.3.2"
    `maven-publish`
//    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.google.devtools.ksp") version "1.8.20-1.0.10" apply false
}

dependencies {
    api(projects.core)
    api(projects.gl)
    api(projects.glfw)
    // vk
}

kotlin.jvmToolchain { languageVersion.set(JavaLanguageVersion.of(8)) }

tasks {
    withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package")
    }
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