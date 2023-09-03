
pluginManagement {
    repositories {
        gradlePluginPortal()
//        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
//        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
    }
//    includeBuild("../build-logic")
}

rootProject.name = "imgui"

for(module in listOf("core", "glfw", "gl"/*,"vk", "jogl",*//* "openjfx"*//*, "bgfx"*/, "platform")) {
    include(module)
    project(":$module").buildFileName = "$module.gradle.kts"
}

//enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradle.rootProject {
    group = "kotlin.graphics"
    version = "1.89.7"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    }
}