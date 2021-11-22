import kx.*
import kx.implementation
import magik.github
import org.lwjgl.Lwjgl.implementation

plugins {
    for (p in listOf("align", "base", "publish", "utils"))
        id("io.github.kotlin-graphics.$p")
    id("org.lwjgl.plugin")
}

repositories { github("kotlin-graphics/mary") }
dependencies {
    implementation(kotlin("reflect"))
    //    implementation(unsigned, kool, glm, gli/*, uno.core*/)
    implementation("kotlin.graphics:uno-core:0.7.10")
    //    org.lwjgl.Lwjgl { implementation(org.lwjgl.Lwjgl.Module.jemalloc, org.lwjgl.Lwjgl.Module.stb) }
}