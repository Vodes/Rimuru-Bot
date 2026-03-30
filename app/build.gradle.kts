plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildconfig)
    application
}

version = "dev"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.jda)
    implementation(libs.jda.ktx)
    implementation(libs.kstore)
    implementation(libs.kstore.file)
    implementation(libs.kotlinx.serialization.json)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "pw.vodes.rimuru.MainKt"
}

buildConfig {
    packageName("pw.vodes.rimuru")
    buildConfigField("VERSION", provider { "${project.version}" })
    buildConfigField("APPNAME", "Rimuru-Bot")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
