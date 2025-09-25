plugins {
    id("application")
    checkstyle
    id("org.sonarqube") version "6.3.1.5724"
    jacoco
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.freefair.lombok") version "8.13.1"
    id("com.github.ben-manes.versions") version "0.52.0"
}

group = "hexlet.code"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.h2database:h2:2.3.232")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("gg.jte:jte:3.2.1")
    implementation("io.javalin:javalin:6.6.0")
    implementation("io.javalin:javalin-bundle:6.6.0")
    implementation("io.javalin:javalin-rendering:6.6.0")

    implementation(platform("com.konghq:unirest-java-bom:4.5.1"))
    implementation("com.konghq:unirest-java-core")
    implementation("com.konghq:unirest-modules-jackson")

    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        csv.required = false
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("app")
        archiveVersion.set("1.0-SNAPSHOT")
        archiveClassifier.set("all")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
