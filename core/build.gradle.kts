import org.gradle.internal.os.OperatingSystem.*

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:kotlin-unsigned:${findProperty("unsignedVersion")}")
    implementation("$kx:kool:${findProperty("koolVersion")}")
    api("$kx:glm:${findProperty("glmVersion")}")
    implementation("$kx:gli:${findProperty("gliVersion")}")
    api("$kx:uno-sdk:${findProperty("unoVersion")}")

    val lwjglNatives = "natives-" + when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-jemalloc", "-stb").forEach {
        implementation("org.lwjgl", "lwjgl$it")
        runtimeOnly("org.lwjgl", "lwjgl$it", classifier = lwjglNatives)
    }
}

tasks {
    compileKotlin.get().destinationDir = compileJava.get().destinationDir
}