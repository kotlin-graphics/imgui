import org.gradle.internal.os.OperatingSystem.*

plugins {
    kotlin("jvm")
}

val moduleName = "${group}.imgui_vk"

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
    listOf("", "-glfw", "-opengl", "-remotery", "-vulkan").forEach {
        implementation("org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}")
        if (it != "-vulkan")
            implementation("org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}:natives-$lwjglNatives")
    }
}

tasks.compileJava {
    // this is needed because we have a separate compile step in this example with the 'module-info.java' is in 'main/java' and the Kotlin code is in 'main/kotlin'
    options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
}