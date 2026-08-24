package uk.gov.justice.digital.hmpps.personrecord.simulation

import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.listFeeder
import io.gatling.javaapi.core.CoreDsl.rampUsersPerSec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.OpenInjectionStep
import io.gatling.javaapi.core.PopulationBuilder
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import uk.gov.justice.digital.hmpps.personrecord.config.AppConfig
import uk.gov.justice.digital.hmpps.personrecord.helper.ApiHelper
import uk.gov.justice.digital.hmpps.personrecord.helper.TokenManager
import java.time.Duration

class CorePersonRecordSimulation : Simulation() {

  val allData = csv("testdata/data.csv").readRecords()
  private var prisonNumber = listFeeder(allData.map { mapOf("prison_number" to it["prison_number"]) }).circular()
  private var crn = listFeeder(allData.map { mapOf("crn" to it["crn"]) }).circular()
  private var defendantId = listFeeder(allData.map { mapOf("defendant_id" to it["defendant_id"]) }).circular()
  private var crnAddress = listFeeder(
    allData.map { mapOf("address_crn" to it["address_crn"], "cpr_address_id" to it["cpr_address_id"]) },
  ).circular()

  private val httpProtocol = http.baseUrl(AppConfig.baseUrl)
    .acceptHeader("application/json").shareConnections()

  // Ramps from 0 up to the target rate over `rampUpDuration`, then holds that target rate for `duration`.
  private fun rampThenHold(targetUsersPerSec: Double): List<OpenInjectionStep> = listOf(
    rampUsersPerSec(0.0).to(targetUsersPerSec).during(Duration.ofSeconds(AppConfig.rampUpDuration)),
    constantUsersPerSec(targetUsersPerSec).during(Duration.ofSeconds(AppConfig.duration)).randomized(),
  )

  private val scnPrisonNumber =
    scenario("prisonNumber")
      .feed(prisonNumber)
      .exec { session -> session.set("sharedToken", TokenManager.getToken()) }
      .exec(ApiHelper.getPrisoners)

  private val scnCrn =
    scenario("crn")
      .feed(crn)
      .exec { session -> session.set("sharedToken", TokenManager.getToken()) }
      .exec(ApiHelper.getCrns)

  private val scnDefendantId =
    scenario("defendantId")
      .feed(defendantId)
      .exec { session -> session.set("sharedToken", TokenManager.getToken()) }
      .exec(ApiHelper.getDefendants)

  private val scnCrnAddress =
    scenario("crnAddress")
      .feed(crnAddress)
      .exec { session -> session.set("sharedToken", TokenManager.getToken()) }
      .exec(ApiHelper.getCrnAddresses)

  init {
    val populations = mutableListOf<PopulationBuilder>()
    populations.add(scnPrisonNumber.injectOpen(rampThenHold(AppConfig.getPrisonNumberUsers.toDouble())))
    populations.add(scnCrn.injectOpen(rampThenHold(AppConfig.getCrnUsers.toDouble())))
    populations.add(scnDefendantId.injectOpen(rampThenHold(AppConfig.getDefendantIdUsers.toDouble())))
    populations.add(scnCrnAddress.injectOpen(rampThenHold(AppConfig.getCrnAddressUsers.toDouble())))
    setUp(*populations.toTypedArray())
      .protocols(httpProtocol)
      .maxDuration(Duration.ofSeconds(AppConfig.rampUpDuration + AppConfig.duration))
      .assertions(
        global().successfulRequests().percent().gt(AppConfig.minSuccessPercentage),
        global().responseTime().percentile(95.0).lt(AppConfig.p95ThresholdMillis),
        global().responseTime().percentile(99.0).lt(AppConfig.p99ThresholdMillis),
      )
  }
}
