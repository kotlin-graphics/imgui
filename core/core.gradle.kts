import magik.createGithubPublication
import magik.github
import org.lwjgl.lwjgl
import org.lwjgl.lwjgl.Module.jemalloc
import org.lwjgl.lwjgl.Module.stb

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

    implementation("kotlin.graphics:uno-core:0.7.17")
    implementation("kotlin.graphics:gln:0.5.31")
    implementation("kotlin.graphics:glm:0.9.9.1-5")
    implementation("kotlin.graphics:gli:0.8.3.0-18")
    implementation("kotlin.graphics:unsigned:3.3.31")
    implementation("kotlin.graphics:kool:0.9.68")
    //    implementation(unsigned, kool, glm, gli/*, uno.core*/)
    lwjgl { implementation(jemalloc, stb) }
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