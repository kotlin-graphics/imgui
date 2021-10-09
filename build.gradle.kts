import kx.*
import kx.Lwjgl
import kx.Lwjgl.Modules.*
import kx.implementation
import org.gradle.internal.os.OperatingSystem.*

plugins {
    val build = "0.7.3+51"
    id("kx.kotlin") version build
    //    id("kx.dokka") version build
    id("kx.publish") version build
    id("kx.dynamic-align") version build
    id("kx.util") version build
}

subprojects {
    apply(plugin = "kx.kotlin")
    apply(plugin = "kx.publish")
    apply(plugin = "kx.dynamic-align")
    apply(plugin = "kx.util")
}

project(":core").dependencies {
    implementation(kotlin("reflect"))
    implementation(unsigned, kool, glm, gli, uno.core)
    Lwjgl { implementation(jemalloc, stb) }
}

project(":gl").dependencies {
    implementation(projects.core)
    implementation(projects.glfw)
    implementation(kotlin("reflect"))
    implementation(unsigned, kool, glm, gli, gln, uno)
    Lwjgl { implementation(jemalloc, glfw, opengl, remotery, stb) }
    testImplementation("com.github.ajalt:mordant:1.2.1")
}

project(":glfw").dependencies {
    implementation(projects.core)
    implementation(kotlin("reflect"))
    implementation(kool, glm, uno)
    Lwjgl { implementation(glfw, opengl, remotery) }
}

project(":openjfx").dependencies {
    implementation(projects.core)
    val platform = when {
        current().isWindows -> "win"
        current().isLinux -> "linux"
        else -> "mac"
    }
    listOf("base", "graphics").forEach {
        implementation("org.openjfx:javafx-$it:11:$platform")
    }
    implementation(glm)
    Lwjgl { implementation() } // just core
}

project(":vk").dependencies {
    implementation(projects.core)
    implementation(projects.glfw)
    implementation(kotlin("reflect"))
    implementation(kool, glm, gli, vkk, uno)
    Lwjgl { implementation(glfw, opengl, remotery, vulkan) }
}