package uk.gov.justice.digital.hmpps.personrecord.config

import com.typesafe.config.ConfigFactory

object AppConfig {
  val clientId: String = System.getenv("CLIENT_ID")
  val clientSecret: String = System.getenv("CLIENT_SECRET")

  private val config = ConfigFactory.load()
    .withFallback(ConfigFactory.load("application.conf"))
    .withFallback(ConfigFactory.load("simulation.conf"))

  val env = System.getProperty("env", "dev")
  val profile: String = System.getProperty("profile", "happypath")
  val duration = System.getProperty("duration", "360").toLong()

  private fun conf(path: String) = config.getAnyRef(path)
  val baseUrl = conf("environments.$env.baseUrl") as String
  val tokenUrl = conf("environments.$env.tokenUrl") as String
  val uriGetPrisoner = conf("endpoint.getPrisoner") as String
  val uriGetCrn = conf("endpoint.getCrn") as String
  val uriGetDefendantId = conf("endpoint.getDefendantId") as String

  val getPrisonNumberUsers = conf("profiles.$profile.getPrisonNumberUsers") as Int
  val getCrnUsers = conf("profiles.$profile.getCrnUsers") as Int
  val getDefendantIdUsers = conf("profiles.$profile.getDefendantIdUsers") as Int
}