import org.gradle.internal.os.OperatingSystem.*

val moduleName = "$group.${rootProject.name}.vk"

dependencies {

    implementation(project(":core"))
    implementation(project(":glfw"))

    implementation(kotlin("reflect"))

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:kool:${findProperty("koolVersion")}")
    implementation("$kx:glm:${findProperty("glmVersion")}")
    implementation("$kx:gli:${findProperty("gliVersion")}")
    implementation("$kx:vkk:${findProperty("vkkVersion")}")
    implementation("$kx:uno-sdk:${findProperty("unoVersion")}")

    val lwjglNatives = "natives-" + when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-glfw", "-opengl", "-remotery", "-vulkan").forEach {
        implementation("org.lwjgl", "lwjgl$it")
        if(it != "-vulkan")
            runtimeOnly("org.lwjgl", "lwjgl$it", classifier = lwjglNatives)
    }
}

tasks.compileJava {
    // this is needed because we have a separate compile step in this example with the 'module-info.java' is in 'main/java' and the Kotlin code is in 'main/kotlin'
    options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
}