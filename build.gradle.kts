import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import java.net.URL

plugins {
    java
    kotlin("jvm") version "1.4.10"
    `maven-publish`
    id("org.jetbrains.dokka") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "6.0.0"
//    idea
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "com.github.kotlin_graphics"

    dependencies {

        implementation(kotlin("stdlib-jdk8"))

        implementation(platform("org.lwjgl:lwjgl-bom:${findProperty("lwjglVersion")}"))

        testImplementation("io.kotest:kotest-runner-junit5-jvm:${findProperty("kotestVersion")}")
        testImplementation("io.kotest:kotest-assertions-core-jvm:${findProperty("kotestVersion")}")
    }

    repositories {
        mavenCentral()
        jcenter()
        maven("https://jitpack.io")
    }

    tasks {

        if (name != "vk")
            dokkaHtml {
                dokkaSourceSets.configureEach {
                    sourceLink {
                        localDirectory.set(file("src/main/kotlin"))
                        remoteUrl.set(URL("https://github.com/kotlin-graphics/glm/tree/master/src/main/kotlin"))
                        remoteLineSuffix.set("#L")
                    }
                }
            }

        withType<KotlinCompile>().all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
            }
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

    java {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.get().outputDirectory.get())
        archiveClassifier.set("javadoc")
    }

    val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
        dependsOn(tasks.dokkaHtml)
        from(tasks.dokkaHtml.get().outputDirectory.get())
        archiveClassifier.set("html-doc")
    }

    val sourceJar = task("sourceJar", Jar::class) {
        dependsOn(tasks.classes)
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    artifacts {
        if (name != "vk") {
            archives(dokkaJavadocJar)
            archives(dokkaHtmlJar)
        }
        archives(sourceJar)
    }

    publishing {
        publications.create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourceJar)
        }
        repositories.maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kotlin-graphics/imgui")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    // == Add access to the 'modular' variant of kotlin("stdlib"): Put this into a buildSrc plugin and reuse it in all your subprojects
    configurations.all { attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8) }
}