plugins {
    kotlin("jvm") version "2.0.10"
    id("io.ktor.plugin") version "2.2.3"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("mysql:mysql-connector-java:5.1.13")
    implementation("org.json:json:20231013") // Для работы с JSON
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

application {
    mainClass.set("org.example.MainKt")
}

ktor {
    fatJar {
        archiveFileName.set("server.jar")
    }
}