
//pluginManagement {
//    repositories {
//        gradlePluginPortal()
//        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
//    }
////    includeBuild("../build-logic")
//}

rootProject.name = "imgui"

for(module in listOf("core", "glfw", "gl", /*"vk", "jogl",*//* "openjfx"*//*, "bgfx"*/)) {
    include(module)
    project(":$module").buildFileName = "$module.gradle.kts"
}

//enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradle.rootProject {
    group = "kotlin.graphics"
    version = "1.79+04"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    }
}