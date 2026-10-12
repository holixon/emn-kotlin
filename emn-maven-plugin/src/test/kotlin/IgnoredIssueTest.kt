package io.holixon.emn.maven

import io.holixon.emn.verify.VerificationIssueCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class IgnoredIssueTest {

  @Test
  fun `converts Maven configuration to a verification rule`() {
    val issue = IgnoredIssue().apply {
      code = "missing_read_model"
      elementId = "GameState"
      reason = "Implemented by the frontend"
    }

    assertThat(issue.toRule()).satisfies({ rule ->
      assertThat(rule.code).isEqualTo(VerificationIssueCode.MISSING_READ_MODEL)
      assertThat(rule.elementId).isEqualTo("GameState")
      assertThat(rule.reason).isEqualTo("Implemented by the frontend")
    })
  }

  @Test
  fun `rejects unknown verification issue codes`() {
    val issue = IgnoredIssue().apply {
      code = "not_an_issue"
      elementId = "GameState"
    }

    assertThatIllegalArgumentException().isThrownBy(issue::toRule)
      .withMessage("Unknown EMN verification issue code 'not_an_issue'.")
  }
}
