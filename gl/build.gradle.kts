import org.gradle.internal.os.OperatingSystem.*

plugins {
    kotlin("jvm")
}

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

    val lwjglNatives = when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-jemalloc", "-glfw", "-opengl", "-remotery", "-stb").forEach {
        implementation("org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}")
        implementation("org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}:natives-$lwjglNatives")
    }

    testImplementation("com.github.ajalt:mordant:1.2.1")
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

//task lightJar(type: Jar) {
//    archiveClassifier = 'light'
//    from sourceSets.main.output
//    exclude 'extraFonts'
//    inputs.property("moduleName", moduleName)
//    manifest {
//        attributes('Automatic-Module-Name': moduleName)
//    }
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}