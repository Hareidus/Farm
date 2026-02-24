import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.2.0"
    id("io.izzel.taboolib") version "2.0.28"
}

version = project.property("version") as String
group = project.property("group") as String



val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitHook)
        install(BukkitNMSUtil)
        install(BukkitUI)
        install(Database)
        install(CommandHelper)
        install(DatabasePlayer)
    }
    version {
        taboolib = "6.2.4-1645904"
        isSkipKotlinRelocate = true
        isSkipKotlin = true
    }
    relocate("EasyLib", "com.hareidus.taboo.farm.libs.EasyLib")
}

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/central")
    maven {
        url = uri("https://nexus.maplex.top/repository/maven-public/")
        isAllowInsecureProtocol = true
    }
}

dependencies {

    // TabooLib / NMS
    compileOnly("ink.ptms.core:v12101:12101:mapped")
    compileOnly("ink.ptms.core:v12101:12101:universal")

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
    taboo(fileTree("depends"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.register("idePostSync") {
    group = "ide"
    description = "IDE post sync task"
}
