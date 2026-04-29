import org.gradle.internal.classpath.Instrumented.systemProperty


plugins {
    kotlin("jvm") version "2.3.0"
    id("io.gatling.gradle") version "3.15.0"
    id("application")
    id("org.owasp.dependencycheck") version "12.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    gatling("org.postgresql:postgresql:42.7.7")
    implementation("io.gatling.highcharts:gatling-charts-highcharts:3.15.0")
    implementation("io.netty:netty-codec-http2:4.1.132.Final")
    implementation("io.netty:netty-handler:4.1.132.Final")
}

kotlin {
    jvmToolchain(25)
}

application{
    mainClass.set("uk.gov.justice.digital.hmpps.personrecord.helper.CsvGenerator")
}

tasks.register<JavaExec>("generateTestData") {
    group = "application"
    classpath = sourceSets.getByName("gatling").runtimeClasspath
    mainClass.set("uk.gov.justice.digital.hmpps.personrecord.helper.CsvGenerator")
}

tasks.register<Exec>("gatlingRunCi") {
    group = "gatling"
    description = "Run un-attended in github ci"

    val args = mutableListOf("gatlingRun")
    args += listOf("--all")

    workingDir = project.rootDir
    val wrapper = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
    println("[GATLING][Gradle] $wrapper ${args.joinToString(" ")}")
    commandLine(wrapper, *args.toTypedArray())
}

gatling {
    systemProperty("profile", System.getProperty("profile") ?: "happypath")
    systemProperty("env", System.getProperty("env") ?: "dev")
}