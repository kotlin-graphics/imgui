import org.gradle.internal.os.OperatingSystem.*


val moduleName = "$group.${rootProject.name}.bgfx"

dependencies {

    implementation(project(":core"))

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:uno-sdk:${findProperty("unoVersion")}")
    implementation("$kx:glm:${findProperty("glmVersion")}")
    implementation("$kx:kool:${findProperty("koolVersion")}")

    val lwjglNatives = "natives-" + when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-glfw", "-bgfx", "-stb").forEach {
        implementation("org.lwjgl", "lwjgl$it")
        if (it != "-jawt")
            runtimeOnly("org.lwjgl", "lwjgl$it", classifier = lwjglNatives)
    }

//    testImplementation group: 'junit', name: 'junit', version: '4.12'
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
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
//    manifest {
//        attributes('Automatic-Module-Name': moduleName)
//    }
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}