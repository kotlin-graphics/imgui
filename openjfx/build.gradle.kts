import org.gradle.internal.os.OperatingSystem.*

val moduleName = "$group.${rootProject.name}.openjfx"

dependencies {

    implementation(project(":core"))

    val (platform, lwjglNatives) = when {
        current().isWindows -> "win" to "windows"
        current().isLinux -> "linux" to "linux"
        else -> "mac" to "macos"
    }
    listOf("base", "graphics").forEach {
        implementation("org.openjfx:javafx-$it:11:$platform")
    }

    implementation("org.lwjgl", "lwjgl")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = "natives-$lwjglNatives")
}

tasks.compileJava {
    // this is needed because we have a separate compile step in this example with the 'module-info.java' is in 'main/java' and the Kotlin code is in 'main/kotlin'
    options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
}