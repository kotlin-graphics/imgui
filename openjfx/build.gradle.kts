import org.gradle.internal.os.OperatingSystem.*

dependencies {

    implementation(project(":core"))

    val platform = when {
        current().isWindows -> "win"
        current().isLinux -> "linux"
        else -> "mac"
    }
    listOf("base", "graphics").forEach {
        implementation("org.openjfx:javafx-$it:11:$platform")
    }

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:glm:${findProperty("glmVersion")}")

    val lwjglNatives = "natives-" + when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }

    implementation("org.lwjgl", "lwjgl")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
}