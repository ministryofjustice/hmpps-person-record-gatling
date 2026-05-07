package uk.gov.justice.digital.hmpps.personrecord.helper


import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.personrecord.config.AppConfig

object ApiHelper {

  val getPrisoners: ChainBuilder =
    exec(
      http("GET Prisoner")
        .get(AppConfig.uriGetPrisoner)
        .header("Authorization", "Bearer #{sharedToken}")
        .check(status().shouldBe(200)),
    )

  val getCrns: ChainBuilder = exec(
    http("GET Crn")
      .get(AppConfig.uriGetCrn)
      .header("Authorization", "Bearer #{sharedToken}")
      .check(status().shouldBe(200)),
  )

  val getDefendants: ChainBuilder = exec(
    http("GET Defendant id")
      .get(AppConfig.uriGetDefendantId)
      .header("Authorization", "Bearer #{sharedToken}")
      .check(status().shouldBe(200)),
  )
}