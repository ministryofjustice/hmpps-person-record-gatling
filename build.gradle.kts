import org.gradle.internal.classpath.Instrumented.systemProperty


plugins {
    kotlin("jvm") version "2.3.21"
    id("io.gatling.gradle") version "3.15.1"
    id("application")
    id("org.owasp.dependencycheck") version "12.2.2"
}

repositories {
    mavenCentral()
}

dependencies {
    gatling("org.postgresql:postgresql:42.7.11")
    implementation("io.gatling.highcharts:gatling-charts-highcharts:3.15.1")
    implementation("io.netty:netty-codec-http2:4.2.14.Final")
    implementation("io.netty:netty-handler:4.2.14.Final")
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
    val profile = System.getProperty("profile") ?: "happypath"
    val env = System.getProperty("env") ?: "dev"
    val duration = System.getProperty("duration") ?: "360"
    workingDir = project.rootDir
    val wrapper = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
    commandLine(wrapper, "gatlingRun", "--all", "-Dprofile=$profile", "-Denv=$env", "-Dduration=$duration")
}
gatling {
    systemProperty("profile", System.getProperty("profile") ?: "happypath")
    systemProperty("env", System.getProperty("env") ?: "dev")
    systemProperty("duration", System.getProperty("duration") ?: "360")
}