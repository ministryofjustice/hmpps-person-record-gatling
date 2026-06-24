package example

import au.com.dius.pact.consumer.dsl.LambdaDsl
import au.com.dius.pact.consumer.dsl.PactBuilder
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import io.restassured.RestAssured
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import org.hamcrest.Matchers.equalTo

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "core-person-record")
class PactConsumerTest {

     private val crn = "X744208"
     private val cprAddressId = "ec4c7479-218c-4f11-a02d-edd749820679"

     @Pact(consumer = "probation-address-consumer")
     fun createPact(builder: PactBuilder): V4Pact {

           val responseBody = LambdaDsl.newJsonBody { body ->
                 body.stringType("cprAddressId", cprAddressId)
                 body.booleanType("noFixedAbode", false)
                 body.stringType("startDate", "2020-02-26")
                 body.stringType("endDate", "2023-07-15")
                 body.stringType("postcode", "SW1H 9AJ")
                 body.stringType("subBuildingName", "Sub building 2")
                 body.stringType("buildingName", "Main Building")
                 body.stringType("buildingNumber", "102")
                 body.stringType("thoroughfareName", "Petty France")
                 body.stringType("dependentLocality", "Westminster")
                 body.stringType("postTown", "London")
                 body.stringType("county", "Greater London")
                 body.stringType("country", "United Kingdom of Great Britain and Northern Ireland (the)")
                 body.stringType("countryCode", "GBR")
                 body.stringType("uprn", "100120991537")
                 body.`object`("status") { statusObj ->
                   statusObj.stringType("code", "M")
                   statusObj.stringType("description", "Main")
                 }
                 body.stringType("comment", "Some comment")
                 body.minArrayLike("usages", 1) { usage ->
                   usage.stringType("description", "CURFEW")
                   usage.stringType("code", "Curfew Order")
                   usage.booleanType("isActive", true)
                 }
                body.minArrayLike("contacts", 1) { contact ->
                    contact.`object`("type") { typeObj ->
                        typeObj.stringType("code", "HOME")
                        typeObj.stringType("description", "Home")
                    }
                    contact.stringType("value", "+44 20 7946 0000")
                    contact.stringType("extension", "1234")
                 }
               }.build()

           return builder
             .usingLegacyDsl()
             .given("An address exists for CRN and address ID")
             .uponReceiving("A request to retrieve probation address")
               .pathFromProviderState("/person/probation/\${crn}/address/\${cprAddressId}","/person/probation/$crn/address/$cprAddressId")
               .method("GET")
               .headers(mapOf("Authorization" to "Bearer test-token"))
             .willRespondWith()
               .status(200)
               .headers(mapOf("Content-Type" to "application/json"))
               .body(responseBody)
             .toPact(V4Pact::class.java)
         }

    @Test
    @PactTestFor
    fun testGetProbationAddress(mockServer: au.com.dius.pact.consumer.MockServer) {
        RestAssured.given()
            .headers(mapOf("Authorization" to "Bearer test-token"))
            .`when`()
            .get("${mockServer.getUrl()}/person/probation/$crn/address/$cprAddressId")
            .then()
            .statusCode(200)
            .body("cprAddressId", equalTo(cprAddressId))
            .body("status.code", equalTo("M"))
            .body("usages[0].code", equalTo("Curfew Order"))
    }
}