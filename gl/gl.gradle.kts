import magik.createGithubPublication
import magik.github
import org.lwjgl.lwjgl
import org.lwjgl.Lwjgl.Module.*

plugins {
    id("org.lwjgl.plugin")
    id("elect86.magik")
    `maven-publish`
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(projects.core)
    implementation(projects.glfw)

    implementation("kotlin.graphics:uno:0.7.17")
    implementation("kotlin.graphics:gln:0.5.31")
    implementation("kotlin.graphics:glm:0.9.9.1-5")
    implementation("kotlin.graphics:gli:0.8.3.0-18")
    implementation("kotlin.graphics:unsigned:3.3.31")
    implementation("kotlin.graphics:kool:0.9.68")
    lwjgl { implementation(jemalloc, glfw, opengl, remotery, stb) }

    testImplementation("com.github.ajalt:mordant:1.2.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.4.1")
    testImplementation("io.kotest:kotest-assertions-core:5.4.1")
}

kotlin.jvmToolchain {
    this as JavaToolchainSpec
    languageVersion.set(JavaLanguageVersion.of(8))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
    withType<Test>().configureEach { useJUnitPlatform() }
}

publishing {
    publications {
        createGithubPublication {
            from(components["java"])
            artifactId = "${rootProject.name}-${project.name}"
            suppressAllPomMetadataWarnings()
        }
    }
    repositories {
        github {
            domain = "kotlin-graphics/mary"
        }
    }
}

java { withSourcesJar() }