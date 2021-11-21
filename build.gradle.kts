import kx.*
import magik.github
import org.gradle.internal.os.OperatingSystem.*
import org.lwjgl.Lwjgl
import org.lwjgl.Lwjgl.Module.*

plugins {
    for ((p, v) in listOf("align" to "0.0.7",
                          "base" to "0.0.10",
                          "publish" to "0.0.7",
                          "utils" to "0.0.5"))
        id("io.github.kotlin-graphics.$p") version v
    id("org.lwjgl.plugin") version "0.0.20"
}

repositories { github("kotlin-graphics/mary") }

//dependencies {
////    implementation("kotlin.graphics:glm:0.9.9.1-4")
////    implementation("kotlin.graphics:uno-core:0.7.10")
//}
//subprojects {
//    version = rootProject.version
//    fun kx(vararg p: String) = p.forEach { apply(plugin = "io.github.kotlin-graphics.$it") }
//    kx("align", "base", "publish", "utils")
//}

//projects.core.dependencyProject.apply {
//    repositories { github("kotlin-graphics/mary") }
//    dependencies {
//        implementation(kotlin("reflect"))
//        implementation(unsigned, kool, glm, gli/*, uno.core*/)
//        //        implementation("kotlin.graphics:uno-core:0.7.10")
//        Lwjgl { implementation(jemalloc, stb) }
//    }
//}

//projects.gl.dependencyProject.dependencies {
//    implementation(projects.core)
//    implementation(projects.glfw)
//    implementation(kotlin("reflect"))
//    implementation(unsigned, kool, glm, gli, gln, uno)
//    Lwjgl { implementation(jemalloc, glfw, opengl, remotery, stb) }
//    testImplementation("com.github.ajalt:mordant:1.2.1")
//}
//
//projects.glfw.dependencyProject.dependencies {
//    implementation(projects.core)
//    implementation(kotlin("reflect"))
//    implementation(kool, glm, uno)
//    Lwjgl { implementation(glfw, opengl, remotery) }
//}
//
//projects.openjfx.dependencyProject.dependencies {
//    implementation(projects.core)
//    val os = current()
//    val platform = when {
//        os.isLinux -> "linux"
//        os.isWindows -> "win"
//        else -> "mac"
//    }
//    listOf("base", "graphics").forEach {
//        implementation("org.openjfx:javafx-$it:11:$platform")
//    }
//    implementation(glm)
//    Lwjgl { implementation(core) }
//}
//
//projects.vk.dependencyProject.dependencies {
//    implementation(projects.core)
//    implementation(projects.glfw)
//    implementation(kotlin("reflect"))
//    implementation(kool, glm, gli, vkk, uno)
//    Lwjgl { implementation(glfw, opengl, remotery, vulkan) }
//}