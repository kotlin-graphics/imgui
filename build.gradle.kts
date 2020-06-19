import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE

plugins {
    java
    `java-library`
    kotlin("jvm") version "1.3.72"
    maven
    id("org.jetbrains.dokka") version "0.10.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    idea
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "com.github.kotlin_graphics"

    dependencies {

        implementation(kotlin("stdlib-jdk8"))

        listOf("runner-junit5", "assertions-core", "runner-console"/*, "property"*/).forEach {
            testImplementation("io.kotest:kotest-$it-jvm:${findProperty("kotestVersion")}")
        }
    }

    repositories {
        mavenCentral()
        jcenter()
        maven { url = uri("https://jitpack.io") }
    }

    tasks {
        val dokka by getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
            outputFormat = "html"
            outputDirectory = "$buildDir/dokka"
        }

        compileKotlin {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf("-XXLanguage:+InlineClasses", "-Xjvm-default=enable")
            }
            sourceCompatibility = "1.8"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
            sourceCompatibility = "1.8"
        }

        withType<Test> { useJUnitPlatform() }

//        task lightJar (type: Jar) {
//            archiveClassifier = 'light'
//            from sourceSets . main . output
//                    exclude 'extraFonts'
//            inputs.property("moduleName", moduleName)
//            manifest {
//                attributes('Automatic-Module-Name': moduleName)
//            }
//            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//        }
    }

    val dokkaJar by tasks.creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assembles Kotlin docs with Dokka"
        archiveClassifier.set("javadoc")
        from(tasks.dokka)
    }

    val sourceJar = task("sourceJar", Jar::class) {
        dependsOn(tasks["classes"])
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    artifacts {
        archives(sourceJar)
        archives(dokkaJar)
    }

    // == Add access to the 'modular' variant of kotlin("stdlib"): Put this into a buildSrc plugin and reuse it in all your subprojects
    configurations.all {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
    }
}