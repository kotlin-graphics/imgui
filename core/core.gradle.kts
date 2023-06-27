import magik.createGithubPublication
import magik.github
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.lwjgl.Lwjgl.Module.jemalloc
import org.lwjgl.Lwjgl.Module.stb
import org.lwjgl.lwjgl

plugins {
    kotlin("jvm")
    id("org.lwjgl.plugin")
    id("elect86.magik")
    `maven-publish`
    id("com.google.devtools.ksp")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))

    api("kotlin.graphics:uno-core:0.7.21")
    lwjgl { implementation(jemalloc, stb) }

    implementation("com.github.livefront.sealed-enum:runtime:0.7.0")
    ksp("com.github.livefront.sealed-enum:ksp:0.7.0")

    val brotliVersion = "1.11.0"
    val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
    runtimeOnly("com.aayushatharva.brotli4j:native-${
        if (operatingSystem.isWindows) "windows-x86_64"
        else if (operatingSystem.isMacOsX)
            if (DefaultNativePlatform.getCurrentArchitecture().isArm()) "osx-aarch64"
            else "osx-x86_64"
        else if (operatingSystem.isLinux)
            if (Architectures.ARM_V7.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-armv7"
            else if (Architectures.AARCH64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-aarch64"
            else if (Architectures.X86_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-x86_64"
            else throw IllegalStateException("Unsupported architecture: ${DefaultNativePlatform.getCurrentArchitecture().name}")
        else throw IllegalStateException("Unsupported operating system: $operatingSystem")
    }:$brotliVersion")
}

kotlin.jvmToolchain { languageVersion.set(JavaLanguageVersion.of(8)) }

tasks {
    withType<KotlinCompilationTask<*>>().configureEach { compilerOptions.freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package") }
    test { useJUnitPlatform() }
}

publishing {
    publications {
        createGithubPublication {
            from(components["java"])
            artifactId = "${rootProject.name}-${project.name}"
            suppressAllPomMetadataWarnings()
        }
    }
    repositories.github { domain = "kotlin-graphics/mary" }
}

java.withSourcesJar()