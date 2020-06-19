import org.gradle.internal.os.OperatingSystem.*


val moduleName = "${group}.imgui_bgfx"

dependencies {

    implementation(project(":imgui-core"))

    val kx = "com.github.kotlin-graphics"
    implementation("$kx:uno-sdk:${findProperty("unoVersion")}")
    implementation("$kx:glm:${findProperty("glmVersion")}")
    implementation("$kx:kool:${findProperty("koolVersion")}")

    val lwjglNatives = when (current()) {
        WINDOWS -> "windows"
        LINUX -> "linux"
        else -> "macos"
    }
    listOf("", "-glfw", "-bgfx", "-stb").forEach {
        val base = "org.lwjgl:lwjgl$it:${findProperty("lwjglVersion")}"
        implementation(base)
        val natives = "$base:natives-$lwjglNatives"
        testRuntimeOnly(natives)
        shadow(natives)
    }

//    testImplementation group: 'junit', name: 'junit', version: '4.12'
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