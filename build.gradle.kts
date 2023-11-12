plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pw.vodes"
version = "0.1"

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


    // These are local and/or not for this bot

    // implementation("pirc:bot:0.1")
    // implementation("pw.vodes:flood4j:0.0.1")
    // implementation("pw.vodes:fileuploaderutility:0.1")

    // https://mvnrepository.com/artifact/com.rometools/rome
    // implementation("com.rometools:rome:2.1.0")
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    // implementation("com.mysql:mysql-connector-j:8.2.0")
    // https://mvnrepository.com/artifact/org.bitbucket.ijabz/jaudiotagger
    // implementation("org.bitbucket.ijabz:jaudiotagger:v2.2.5")
    // https://mvnrepository.com/artifact/org.yaml/snakeyaml
    // implementation("org.yaml:snakeyaml:2.2")
    // https://mvnrepository.com/artifact/de.androidpit/color-thief
    // implementation("de.androidpit:color-thief:1.1.2")
    // https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j
    // implementation("net.lingala.zip4j:zip4j:2.11.5")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    // implementation("com.google.code.gson:gson:2.8.9")
    // https://mvnrepository.com/artifact/org.json/json
    // implementation("org.json:json:20231013")

    // testImplementation(kotlin("test"))
}

kotlin {
    if (System.getProperty("os.name").startsWith("win", true))
        jvmToolchain(17)
    else
        jvmToolchain(11)
}

application {
    mainClass.set("pw.vodes.rimurukt.MainKt")
}