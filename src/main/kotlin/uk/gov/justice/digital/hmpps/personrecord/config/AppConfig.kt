package uk.gov.justice.digital.hmpps.personrecord.config

import com.typesafe.config.ConfigFactory

object AppConfig {
  val clientId: String = System.getenv("CLIENT_ID")
  val clientSecret: String = System.getenv("CLIENT_SECRET")

  private val config = ConfigFactory.load()
    .withFallback(ConfigFactory.load("application.conf"))

  val env = System.getProperty("env", "dev")
  val duration = System.getProperty("duration", "360").toLong()

  private fun conf(path: String) = config.getAnyRef(path)
  val baseUrl = conf("environments.$env.baseUrl") as String
  val tokenUrl = conf("environments.$env.tokenUrl") as String
  val uriGetPrisoner = conf("endpoint.getPrisoner") as String
  val uriGetCrn = conf("endpoint.getCrn") as String
  val uriGetDefendantId = conf("endpoint.getDefendantId") as String

  val getPrisonNumberUsers = System.getProperty("getPrisonNumber", "15").toInt()
  val getCrnUsers = System.getProperty("getCrnNumber", "1").toInt()
  val getDefendantIdUsers = System.getProperty("getDefendantId", "1").toInt()

  // Response-time SLA thresholds (milliseconds) and minimum success rate (%), used as Gatling assertions.
  val p95ThresholdMillis = System.getProperty("p95ThresholdMillis", "1000").toInt()
  val p99ThresholdMillis = System.getProperty("p99ThresholdMillis", "2000").toInt()
  val minSuccessPercentage = System.getProperty("minSuccessPercentage", "95.0").toDouble()
}
