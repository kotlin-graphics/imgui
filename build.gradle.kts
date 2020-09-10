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

    java {
        modularity.inferModulePath.set(true)
    }

    dependencies {

        implementation(kotlin("stdlib"))
        implementation(kotlin("stdlib-jdk8"))

        attributesSchema.attribute(LIBRARY_ELEMENTS_ATTRIBUTE).compatibilityRules.add(ModularJarCompatibilityRule::class)
        components { withModule<ModularKotlinRule>(kotlin("stdlib")) }
        components { withModule<ModularKotlinRule>(kotlin("stdlib-jdk8")) }

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
                jvmTarget = "11"
                freeCompilerArgs = listOf("-XXLanguage:+InlineClasses", "-Xjvm-default=enable")
            }
            sourceCompatibility = "11"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "11"
            sourceCompatibility = "11"
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
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
        val n = name.toLowerCase()
        if (n.endsWith("compileclasspath") || n.endsWith("runtimeclasspath"))
            attributes.attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named("modular-jar"))
        if (n.endsWith("compile") || n.endsWith("runtime"))
            isCanBeConsumed = false
    }
}

abstract class ModularJarCompatibilityRule : AttributeCompatibilityRule<LibraryElements> {
    override fun execute(details: CompatibilityCheckDetails<LibraryElements>): Unit = details.run {
        if (producerValue?.name == JAR && consumerValue?.name == "modular-jar")
            compatible()
    }
}

abstract class ModularKotlinRule : ComponentMetadataRule {

    @javax.inject.Inject
    abstract fun getObjects(): ObjectFactory

    override fun execute(ctx: ComponentMetadataContext) {
        val id = ctx.details.id
        listOf("compile", "runtime").forEach { baseVariant ->
            ctx.details.addVariant("${baseVariant}Modular", baseVariant) {
                attributes {
                    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, getObjects().named("modular-jar"))
                }
                withFiles {
                    removeAllFiles()
                    addFile("${id.name}-${id.version}-modular.jar")
                }
                withDependencies {
                    clear() // 'kotlin-stdlib-common' and  'annotations' are not modules and are also not needed
                }
            }
        }
    }
}