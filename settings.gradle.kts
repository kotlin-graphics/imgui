
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    }
    includeBuild("../build-logic")
}

rootProject.name = "imgui"

include("core", "glfw", "gl", "vk", /*"jogl",*/ "openjfx"/*, "bgfx"*/)

//enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradle.rootProject {
    group = "kotlin.graphics"
    version = "1.79+04"
}