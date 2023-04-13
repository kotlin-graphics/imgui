
import magik.createGithubPublication
import magik.github
import org.gradle.nativeplatform.platform.internal.Architectures
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.lwjgl.Lwjgl.Module.jemalloc
import org.lwjgl.Lwjgl.Module.stb
import org.lwjgl.lwjgl

plugins {
    id("org.lwjgl.plugin")
    id("elect86.magik")
    `maven-publish`
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("com.google.devtools.ksp")
}

group = rootProject.group
version = rootProject.version

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))

    implementation("kotlin.graphics:uno-core:0.7.17")
    implementation("kotlin.graphics:gln:0.5.31")
    implementation("kotlin.graphics:glm:0.9.9.1-5")
    implementation("kotlin.graphics:gli:0.8.3.0-18")
    implementation("kotlin.graphics:unsigned:3.3.31")
    implementation("kotlin.graphics:kool:0.9.68")
    //    implementation(unsigned, kool, glm, gli/*, uno.core*/)
    lwjgl { implementation(jemalloc, stb) }

    // Temporarily use a commit-hash for the version until the "is checks for equality" change is released
    implementation("com.github.livefront.sealed-enum:runtime:f690fca874")
    ksp("com.github.livefront.sealed-enum:ksp:f690fca874")

    val brotliVersion = "1.11.0"
    val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
//    testImplementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
    runtimeOnly("com.aayushatharva.brotli4j:native-${
        if (operatingSystem.isWindows) "windows-x86_64"
        else if (operatingSystem.isMacOsX)
            if (DefaultNativePlatform.getCurrentArchitecture().isArm()) "osx-aarch64"
            else "osx-x86_64"
        else if (operatingSystem.isLinux)
            if (Architectures.ARM_V7.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-armv7"
            else if (Architectures.AARCH64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-aarch64"
            else if (Architectures.X86_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-x86_64"
            else
                throw IllegalStateException("Unsupported architecture: ${DefaultNativePlatform.getCurrentArchitecture().name}")
        else
            throw IllegalStateException("Unsupported operating system: $operatingSystem")
    }:$brotliVersion")
//    testRuntimeOnly("com.aayushatharva.brotli4j:native-${
//        if (operatingSystem.isWindows) "windows-x86_64"
//        else if (operatingSystem.isMacOsX)
//            if (DefaultNativePlatform.getCurrentArchitecture().isArm()) "osx-aarch64"
//            else "osx-x86_64"
//        else if (operatingSystem.isLinux)
//            if (Architectures.ARM_V7.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-armv7"
//            else if (Architectures.AARCH64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-aarch64"
//            else if (Architectures.X86_64.isAlias(DefaultNativePlatform.getCurrentArchitecture().name)) "linux-x86_64"
//            else
//                throw IllegalStateException("Unsupported architecture: ${DefaultNativePlatform.getCurrentArchitecture().name}")
//        else
//            throw IllegalStateException("Unsupported operating system: $operatingSystem")
//    }:$brotliVersion")
}

kotlin.jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
        kotlinOptions {
            freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn", "-Xallow-kotlin-package")
        }
    }
    withType<Test>().configureEach { useJUnitPlatform() }
}

publishing {
    publications {
        createGithubPublication {
            from(components["java"])
            artifactId = "${rootProject.name}-${project.name}"
            suppressAllPomMetadataWarnings()
        }
    }
    repositories {
        github {
            domain = "kotlin-graphics/mary"
        }
    }
}

java { withSourcesJar() }