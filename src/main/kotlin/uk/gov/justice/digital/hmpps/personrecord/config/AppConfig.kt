package uk.gov.justice.digital.hmpps.personrecord.config

import com.typesafe.config.ConfigFactory

object AppConfig {
  val clientId: String = System.getenv("CLIENT_ID") ?: ""
  val clientSecret: String = System.getenv("CLIENT_SECRET") ?: ""

  private val config = ConfigFactory.load()
    .withFallback(ConfigFactory.load("application.conf"))

  internal fun parseIntProperty(
    name: String,
    rawValue: String?,
    default: Int,
    min: Int? = null,
    max: Int? = null,
  ): Int {
    val value = rawValue?.trim().takeUnless { it.isNullOrEmpty() } ?: default.toString()
    val parsed = value.toIntOrNull()
      ?: throw IllegalArgumentException("System property '$name' must be an integer, but was '$value'")
    validateRange(name, parsed.toDouble(), min?.toDouble(), max?.toDouble())
    return parsed
  }

  internal fun parseLongProperty(
    name: String,
    rawValue: String?,
    default: Long,
    min: Long? = null,
    max: Long? = null,
  ): Long {
    val value = rawValue?.trim().takeUnless { it.isNullOrEmpty() } ?: default.toString()
    val parsed = value.toLongOrNull()
      ?: throw IllegalArgumentException("System property '$name' must be a whole number, but was '$value'")
    validateRange(name, parsed.toDouble(), min?.toDouble(), max?.toDouble())
    return parsed
  }

  internal fun parseDoubleProperty(
    name: String,
    rawValue: String?,
    default: Double,
    min: Double? = null,
    max: Double? = null,
  ): Double {
    val value = rawValue?.trim().takeUnless { it.isNullOrEmpty() } ?: default.toString()
    val parsed = value.toDoubleOrNull()
      ?: throw IllegalArgumentException("System property '$name' must be a number, but was '$value'")
    validateRange(name, parsed, min, max)
    return parsed
  }

  private fun validateRange(name: String, value: Double, min: Double?, max: Double?) {
    if (min != null && value < min) {
      throw IllegalArgumentException("System property '$name' must be >= $min, but was '$value'")
    }
    if (max != null && value > max) {
      throw IllegalArgumentException("System property '$name' must be <= $max, but was '$value'")
    }
  }

  val env = System.getProperty("env", "dev")
  val duration = parseLongProperty("duration", System.getProperty("duration"), 360, min = 1)

  // Ramp-up period (seconds) before each scenario reaches its steady-state target rate.
  val rampUpDuration = parseLongProperty("rampUpDuration", System.getProperty("rampUpDuration"), 30, min = 0)

  private fun conf(path: String) = config.getAnyRef(path)
  val baseUrl = conf("environments.$env.baseUrl") as String
  val tokenUrl = conf("environments.$env.tokenUrl") as String
  val uriGetPrisoner = conf("endpoint.getPrisoner") as String
  val uriGetCrn = conf("endpoint.getCrn") as String
  val uriGetDefendantId = conf("endpoint.getDefendantId") as String
  val uriGetCrnAddress = conf("endpoint.getCrnAddress") as String

  val getPrisonNumberUsers = parseIntProperty("getPrisonNumber", System.getProperty("getPrisonNumber"), 15, min = 0)
  val getCrnUsers = parseIntProperty("getCrnNumber", System.getProperty("getCrnNumber"), 1, min = 0)
  val getDefendantIdUsers = parseIntProperty("getDefendantId", System.getProperty("getDefendantId"), 1, min = 0)
  val getCrnAddressUsers = parseIntProperty("getCrnAddress", System.getProperty("getCrnAddress"), 1, min = 0)

  // Response-time SLA thresholds (milliseconds) and minimum success rate (%), used as Gatling assertions.
  val p95ThresholdMillis =
    parseIntProperty("p95ThresholdMillis", System.getProperty("p95ThresholdMillis"), 1000, min = 1)
  val p99ThresholdMillis =
    parseIntProperty("p99ThresholdMillis", System.getProperty("p99ThresholdMillis"), 2500, min = 1)
  val minSuccessPercentage =
    parseDoubleProperty(
      "minSuccessPercentage",
      System.getProperty("minSuccessPercentage"),
      95.0,
      min = 0.0,
      max = 100.0,
    )
}
