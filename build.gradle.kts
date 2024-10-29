plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "pw.vodes"
version = "1.3.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
    implementation("org.javacord:javacord:3.8.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
    runtimeOnly("org.apache.logging.log4j:log4j-to-slf4j:2.23.1")

    // https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
    // https://mvnrepository.com/artifact/org.unbescape/unbescape
    implementation("org.unbescape:unbescape:1.1.6.RELEASE")
    // https://mvnrepository.com/artifact/com.apptasticsoftware/rssreader
    implementation("com.apptasticsoftware:rssreader:3.6.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("pw.vodes.rimurukt.MainKt")
}

buildConfig {
    buildConfigField("VERSION", provider { "${project.version}" })
    buildConfigField("APPNAME", "Rimuru-Bot")
}