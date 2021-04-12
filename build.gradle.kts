import kx.KxProject.*
import kx.LwjglModules.*
import kx.kxImplementation
import kx.lwjglImplementation
import org.gradle.internal.os.OperatingSystem.*

plugins {
    val build = "0.7.0+100"
    id("kx.kotlin.11") version build
    id("kx.lwjgl") version build
    id("kx.dokka") version build
    id("kx.publish") version build
}

version = "1.79+04"

project(":core").dependencies {
    implementation(kotlin("reflect"))
    kxImplementation(unsigned, kool, glm, gli, unoCore)
    lwjglImplementation(jemalloc, stb)
}

project(":gl").dependencies {
    implementation(rootProject.projects.core)
    implementation(rootProject.projects.glfw)
    implementation(kotlin("reflect"))
    kxImplementation(unsigned, kool, glm, gli, gln, unoCore)
    lwjglImplementation(jemalloc, glfw, opengl, remotery, stb)
    testImplementation("com.github.ajalt:mordant:1.2.1")
}

project(":glfw").dependencies {
    implementation(rootProject.projects.core)
    implementation(kotlin("reflect"))
    kxImplementation(kool, glm, uno)
    lwjglImplementation(glfw, opengl, remotery)
}

project(":openjfx").dependencies {
    implementation(rootProject.projects.core)
    val platform = when {
        current().isWindows -> "win"
        current().isLinux -> "linux"
        else -> "mac"
    }
    listOf("base", "graphics").forEach {
        implementation("org.openjfx:javafx-$it:11:$platform")
    }
    kxImplementation(glm)
    lwjglImplementation() // just core
}

project(":vk").dependencies {
    implementation(rootProject.projects.core)
    implementation(rootProject.projects.glfw)
    implementation(kotlin("reflect"))
    kxImplementation(kool, glm, gli, vkk, uno)
    lwjglImplementation(glfw, opengl, remotery, vulkan)
}