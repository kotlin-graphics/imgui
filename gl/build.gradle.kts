import org.gradle.internal.os.OperatingSystem.*

dependencies {

    implementation(project(":core"))
    implementation(project(":glfw"))

    implementation(kotlin("reflect"))

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:kotlin-unsigned:${findProperty("unsignedVersion")}")
    implementation("$kx:kool:${findProperty("koolVersion")}")
    implementation("$kx:glm:${findProperty("glmVersion")}")
    implementation("$kx:gli:${findProperty("gliVersion")}")
    implementation("$kx:gln:${findProperty("glnVersion")}")
    implementation("${kx}.uno-sdk:core:${findProperty("unoVersion")}")

    val lwjglNatives = "natives-" + when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-jemalloc", "-glfw", "-opengl", "-remotery", "-stb").forEach {
        implementation("org.lwjgl", "lwjgl$it")
        runtimeOnly("org.lwjgl", "lwjgl$it", classifier = lwjglNatives)
    }

    testImplementation("com.github.ajalt:mordant:1.2.1")
}