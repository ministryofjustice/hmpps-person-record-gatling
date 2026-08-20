package uk.gov.justice.digital.hmpps.personrecord.helper

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TokenManagerTest {

  @Test
  fun `parses access token and expiry from a well formed token response`() {
    val body = """{"access_token":"abc123","expires_in":3600,"token_type":"bearer"}"""

    val result = TokenManager.parseTokenResponse(body)

    assertThat(result.accessToken).isEqualTo("abc123")
    assertThat(result.expiresIn).isEqualTo(3600L)
  }

  @Test
  fun `defaults expiresIn to 3600 seconds when not present in the response`() {
    val body = """{"access_token":"abc123"}"""

    val result = TokenManager.parseTokenResponse(body)

    assertThat(result.expiresIn).isEqualTo(3600L)
  }

  @Test
  fun `is not affected by field ordering or additional unknown fields`() {
    val body = """{"token_type":"bearer","expires_in":120,"scope":"read write","access_token":"xyz789"}"""

    val result = TokenManager.parseTokenResponse(body)

    assertThat(result.accessToken).isEqualTo("xyz789")
    assertThat(result.expiresIn).isEqualTo(120L)
  }

  @Test
  fun `throws when access_token is missing`() {
    val body = """{"expires_in":3600}"""

    assertThatThrownBy { TokenManager.parseTokenResponse(body) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("access_token")
  }

  @Test
  fun `throws when access_token is blank`() {
    val body = """{"access_token":"","expires_in":3600}"""

    assertThatThrownBy { TokenManager.parseTokenResponse(body) }
      .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `throws when body is not valid json`() {
    val body = "not json"

    assertThatThrownBy { TokenManager.parseTokenResponse(body) }
      .isInstanceOf(Exception::class.java)
  }
}
