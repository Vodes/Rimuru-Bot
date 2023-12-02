plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pw.vodes"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
    implementation("org.javacord:javacord:3.8.0")

    // https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
    // https://mvnrepository.com/artifact/org.unbescape/unbescape
    implementation("org.unbescape:unbescape:1.1.6.RELEASE")
    // https://mvnrepository.com/artifact/com.apptasticsoftware/rssreader
    implementation("com.apptasticsoftware:rssreader:3.5.0")

    // https://mvnrepository.com/artifact/com.rometools/rome
    // implementation("com.rometools:rome:2.1.0")
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    // implementation("com.mysql:mysql-connector-j:8.2.0")
    // https://mvnrepository.com/artifact/org.bitbucket.ijabz/jaudiotagger
    // implementation("org.bitbucket.ijabz:jaudiotagger:v2.2.5")
    // https://mvnrepository.com/artifact/de.androidpit/color-thief
    // implementation("de.androidpit:color-thief:1.1.2")

    // testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("pw.vodes.rimurukt.MainKt")
}