import magik.createGithubPublication
import magik.github
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.lwjgl.Lwjgl.Module.glfw
import org.lwjgl.Lwjgl.Module.opengl
import org.lwjgl.lwjgl

plugins {
    kotlin("jvm")
    id("org.lwjgl.plugin")
    id("elect86.magik")
    `maven-publish`
    id("com.google.devtools.ksp")
}

group = rootProject.group
version = rootProject.version

dependencies {
//    implementation(kotlin("reflect"))

    implementation(projects.core)
    implementation(projects.gl)
    implementation(projects.glfw)

//    api("kotlin.graphics:uno:0.7.21")
    lwjgl { testImplementation(glfw, opengl/*, remotery*/) }

    implementation("com.github.livefront.sealed-enum:runtime:0.7.0")
    ksp("com.github.livefront.sealed-enum:ksp:0.7.0")

//    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
//    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
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