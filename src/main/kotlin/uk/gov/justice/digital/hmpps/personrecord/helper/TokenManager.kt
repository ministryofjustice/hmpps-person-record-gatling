package uk.gov.justice.digital.hmpps.personrecord.helper

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.personrecord.config.AppConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.Base64

data class TokenResponse(val accessToken: String, val expiresIn: Long)

object TokenManager {
  @Volatile
  private var cacheToken: String = ""

  @Volatile
  private var expiryTime: Long = 0
  private val client = HttpClient.newHttpClient()
  private val objectMapper = ObjectMapper()

  // Parses an OAuth2 client-credentials token response body, e.g.
  // {"access_token":"...","expires_in":3600,...}
  // Exposed internally so parsing logic can be unit tested without a live token endpoint.
  internal fun parseTokenResponse(body: String): TokenResponse {
    val node = objectMapper.readTree(body)
    val accessToken = node.get("access_token")?.asText()
      ?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("Token response did not contain an access_token: $body")
    val expiresIn = node.get("expires_in")?.asLong() ?: 3600L
    return TokenResponse(accessToken, expiresIn)
  }

  @Synchronized
  fun getToken(): String {
    val now = Instant.now().epochSecond
    if (cacheToken.isEmpty() || now >= expiryTime - 300) {
      fetchToken()
    }
    return cacheToken
  }

  private fun fetchToken() {
    if (AppConfig.clientId.isBlank() || AppConfig.clientSecret.isBlank()) {
      throw IllegalStateException("Client credentials not configured - clientId and clientSecret must be provided")
    }

    val basicAuth =
      Base64.getEncoder().encodeToString("${AppConfig.clientId.trim()}:${AppConfig.clientSecret.trim()}".toByteArray())

    val request = HttpRequest.newBuilder().uri(URI.create(AppConfig.tokenUrl))
      .header("Authorization", "Basic $basicAuth")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
      .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() == 200) {
      val tokenResponse = parseTokenResponse(response.body())
      cacheToken = tokenResponse.accessToken
      expiryTime = Instant.now().epochSecond + tokenResponse.expiresIn
    } else {
      throw RuntimeException("Token service error: ${response.statusCode()}")
    }
  }
}
