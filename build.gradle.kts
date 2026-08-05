import org.gradle.internal.classpath.Instrumented.systemProperty


plugins {
    id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.2"
    kotlin("jvm") version "2.4.10"
    id("io.gatling.gradle") version "3.15.1.2"
    id("application")
    id("org.owasp.dependencycheck") version "12.2.2"
}

repositories {
    mavenCentral()
}

dependencies {
    gatling("org.postgresql:postgresql:42.7.13")
    implementation("io.gatling.highcharts:gatling-charts-highcharts:3.15.1")
    implementation("io.netty:netty-codec-http2:4.2.17.Final")
    implementation("io.netty:netty-handler:4.2.17.Final")
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
    val getPrisonNumber = System.getProperty("getPrisonNumber") ?: "15"
    val getCrnNumber = System.getProperty("getCrnNumber") ?: "1"
    val getDefendantId = System.getProperty("getDefendantId") ?: "1"
    val env = System.getProperty("env") ?: "dev"
    val duration = System.getProperty("duration") ?: "360"
    workingDir = project.rootDir
    val wrapper = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
    commandLine(wrapper, "gatlingRun", "--all", "-DgetPrisonNumber=$getPrisonNumber",
      "-DgetCrnNumber=$getCrnNumber", "-DgetDefendantId=$getDefendantId", "-Denv=$env", "-Dduration=$duration")
}
gatling {
    systemProperty("getPrisonNumber", System.getProperty("getPrisonNumber") ?: "15")
    systemProperty("getCrnNumber", System.getProperty("getCrnNumber") ?: "1")
    systemProperty("getDefendantId", System.getProperty("getDefendantId") ?: "1")
    systemProperty("env", System.getProperty("env") ?: "dev")
    systemProperty("duration", System.getProperty("duration") ?: "360")
}