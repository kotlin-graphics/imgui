import org.gradle.internal.os.OperatingSystem.*

plugins {
    kotlin("jvm")
}

val moduleName = "${group}.imgui_openjfx"

dependencies {

    implementation(project(":imgui-core"))

    val platform = when {
        current().isWindows() -> "win"
        current().isLinux() -> "linux"
        else -> "mac"
    }
    listOf("base", "graphics").forEach {
        implementation("org.openjfx:javafx-$it:11:$platform")
    }

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:glm:${findProperty("glmVersion")}")

    val lwjglNatives = when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("").forEach {
        val natives = "org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}:natives-$lwjglNatives"
        runtimeOnly(natives)
        shadow(natives)
    }
}

tasks.compileJava {
    // this is needed because we have a separate compile step in this example with the 'module-info.java' is in 'main/java' and the Kotlin code is in 'main/kotlin'
    options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
}

//task lightJar(type: Jar) {
//    archiveClassifier = 'light'
//    from sourceSets.main.output
//    exclude 'extraFonts'
//    inputs.property("moduleName", moduleName)
////    manifest.attributes('Automatic-Module-Name': moduleName)
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}