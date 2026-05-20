import org.gradle.internal.classpath.Instrumented.systemProperty


plugins {
    kotlin("jvm") version "2.3.0"
    id("io.gatling.gradle") version "3.15.0"
    id("application")
    id("org.owasp.dependencycheck") version "12.2.1"
    id("au.com.dius.pact") version "4.6.14"
}

repositories {
    mavenCentral()
}

dependencies {
    gatling("org.postgresql:postgresql:42.7.11")
    implementation("io.gatling.highcharts:gatling-charts-highcharts:3.15.0")
    implementation("io.netty:netty-codec-http2:4.1.132.Final")
    implementation("io.netty:netty-handler:4.1.132.Final")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("au.com.dius.pact.consumer:junit5:4.6.14")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
tasks.test{
    useJUnitPlatform()
    systemProperty("pact.rootDir", "$buildDir/pacts")
}
gatling {
    systemProperty("profile", System.getProperty("profile") ?: "happypath")
    systemProperty("env", System.getProperty("env") ?: "dev")
    systemProperty("duration", System.getProperty("duration") ?: "360")
}