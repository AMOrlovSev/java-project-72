plugins {
    id("application")
    checkstyle
    id("org.sonarqube") version "6.3.1.5724"
}

group = "hexlet.code"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "hexlet.code.App"
}

sonar {
    properties {
        property("sonar.projectKey", "AMOrlovSev_java-project-72")
        property("sonar.organization", "amorlovsev")
    }
}

tasks.test {
    useJUnitPlatform()
}
