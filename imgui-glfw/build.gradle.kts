import org.gradle.internal.os.OperatingSystem.*

plugins {
    kotlin("jvm")
}

val moduleName = "${group}.imgui_glfw"

dependencies {

    implementation(project(":imgui-core"))

    implementation(kotlin("reflect"))

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:kool:${findProperty("koolVersion")}")
    implementation("$kx:glm:${findProperty("glmVersion")}")
    implementation("$kx:uno-sdk:${findProperty("unoVersion")}")

    val lwjglNatives = when (current()) {
        WINDOWS -> "windows"
            LINUX -> "linux"
            else -> "macos"
    }
    listOf("", "-glfw", "-opengl", "-remotery").forEach {
        implementation("org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}")
        implementation("org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}:natives-$lwjglNatives")
    }
}