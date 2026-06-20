plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.shadow)
    application
}

version = "dev"

repositories {
    mavenCentral()
    maven("https://repo.styx.moe/releases")
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
    implementation(libs.rssreader)
    implementation(libs.unbescape)
    implementation(libs.styx.db)
    implementation(libs.anilist.kmp)
    implementation(libs.jdbc.postgre)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

base {
    archivesName = rootProject.name.lowercase()
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

val shadowJar = tasks.named<Jar>("shadowJar")

tasks.named<Delete>("clean") {
    delete(layout.projectDirectory.file("app.jar"))
}

tasks.register<Copy>("shadowCI") {
    description = "Builds the shadow jar as 'app.jar' in the project root."
    dependsOn(shadowJar)
    from(shadowJar.flatMap { it.archiveFile })
    into(layout.projectDirectory)
    rename { "app.jar" }
}
