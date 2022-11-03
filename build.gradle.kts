import magik.github

plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("org.lwjgl.plugin") version "0.0.29"
    id("elect86.magik") version "0.3.1"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.2"
}


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