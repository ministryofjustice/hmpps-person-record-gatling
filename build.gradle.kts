import org.gradle.internal.classpath.Instrumented.systemProperty
import org.springframework.boot.gradle.tasks.bundling.BootJar


plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.6"
  kotlin("jvm") version "2.4.10"
  id("io.gatling.gradle") version "3.15.1.3"
  id("application")
  id("org.owasp.dependencycheck") version "13.0.0"
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

dependencyCheck {
  // Additive project-specific suppressions, alongside the plugin-managed suppression file.
  suppressionFiles.add("owasp-suppressions.xml")
  // Keep the dependency-check report files at the root of build/reports so the shared
  // GitHub Action can sanitise and upload them without any repo-specific overrides.
  outputDirectory.set(layout.buildDirectory.dir("reports"))
  formats = listOf("HTML", "SARIF")
}

application {
  mainClass.set("uk.gov.justice.digital.hmpps.personrecord.helper.CsvGenerator")
}

// Ensure `assemble` produces a single, predictably-named jar in build/libs (regardless of
// project version or BUILD_NUMBER), since downstream steps (e.g. the Veracode scan) expect
// exactly one file named `${project.name}.jar`.
tasks.named("jar") {
  enabled = false
}

tasks.named<BootJar>("bootJar") {
  archiveFileName.set("${project.name}.jar")
}

// The `copyAgent` task (from the hmpps-gradle-spring-boot plugin) writes the App Insights agent jar
// into build/libs, which bootStartScripts/startScripts also read from without an explicit dependency declared.
tasks.named("bootStartScripts") {
  dependsOn("copyAgent")
}

tasks.named("startScripts") {
  dependsOn("copyAgent")
}

tasks.register<JavaExec>("generateTestData") {
  group = "application"
  description = "Generates src/main/resources/testdata/data.csv by querying prison numbers, " +
    "CRNs and defendant IDs from the database (see CsvGenerator)."
  classpath = sourceSets.getByName("gatling").runtimeClasspath
  mainClass.set("uk.gov.justice.digital.hmpps.personrecord.helper.CsvGenerator")
}

tasks.register<Exec>("gatlingRunCi") {
  group = "gatling"
  description = "Runs the Gatling simulation with CI-friendly defaults for user counts, " +
    "target environment and duration, each overridable via -D system properties."
  val getPrisonNumber = System.getProperty("getPrisonNumber") ?: "15"
  val getCrnNumber = System.getProperty("getCrnNumber") ?: "1"
  val getDefendantId = System.getProperty("getDefendantId") ?: "1"
  val env = System.getProperty("env") ?: "dev"
  val duration = System.getProperty("duration") ?: "360"
  val rampUpDuration = System.getProperty("rampUpDuration") ?: "30"
  workingDir = project.rootDir
  val wrapper = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
  commandLine(
    wrapper,
    "gatlingRun",
    "--all",
    "-DgetPrisonNumber=$getPrisonNumber",
    "-DgetCrnNumber=$getCrnNumber",
    "-DgetDefendantId=$getDefendantId",
    "-Denv=$env",
    "-Dduration=$duration",
    "-DrampUpDuration=$rampUpDuration",
  )
}
gatling {
  systemProperty("getPrisonNumber", System.getProperty("getPrisonNumber") ?: "15")
  systemProperty("getCrnNumber", System.getProperty("getCrnNumber") ?: "1")
  systemProperty("getDefendantId", System.getProperty("getDefendantId") ?: "1")
  systemProperty("env", System.getProperty("env") ?: "dev")
  systemProperty("duration", System.getProperty("duration") ?: "360")
  systemProperty("rampUpDuration", System.getProperty("rampUpDuration") ?: "30")
}
