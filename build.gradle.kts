import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    kotlin("jvm") version "1.5.31"
    application
    id("com.jetbrains.exposed.gradle.plugin") version "0.2.1"
}

group = "dev.bettercoding"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation("org.flywaydb:flyway-core:8.0.1")
    implementation("org.postgresql:postgresql:42.2.24")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.jetbrains.exposed:exposed-core:0.35.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.35.2")
}

exposedCodeGeneratorConfig {
    val dbProperties = loadProperties("${projectDir}/db.properties")
    configFilename = "exposedConf.yml"
    user = dbProperties["dataSource.user"].toString()
    password = dbProperties["dataSource.password"].toString()
    databaseName = dbProperties["dataSource.database"].toString()
    databaseDriver = dbProperties["dataSource.driver"].toString()
}

sourceSets.main {
    java.srcDirs("build/tables")
}

tasks.generateExposedCode {
    dependsOn("clean")
}
