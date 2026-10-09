package io.holixon.emn.verify

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class VerificationIgnorePolicyTest {

  @Test
  fun `ignores only issues matching code and element ID`() {
    val matchingIssue = issue(VerificationIssueCode.MISSING_READ_MODEL, "GameState")
    val otherIssue = issue(VerificationIssueCode.MISSING_EVENT_HANDLER, "GameState")
    val rule = VerificationIgnoreRule(
      VerificationIssueCode.MISSING_READ_MODEL,
      "GameState",
      "Implemented by the frontend",
    )

    val result = VerificationIgnorePolicy.apply(VerificationReport(listOf(matchingIssue, otherIssue)), listOf(rule))

    assertThat(result.report.issues).containsExactly(otherIssue)
    assertThat(result.ignoredIssues.single().issue).isEqualTo(matchingIssue)
    assertThat(result.ignoredIssues.single().rule).isEqualTo(rule)
    assertThat(result.unmatchedRules).isEmpty()
  }

  @Test
  fun `reports rules that match no issue`() {
    val rule = VerificationIgnoreRule(VerificationIssueCode.MISSING_READ_MODEL, "FrontendQuery")

    val result = VerificationIgnorePolicy.apply(VerificationReport(emptyList()), listOf(rule))

    assertThat(result.unmatchedRules).containsExactly(rule)
  }

  @Test
  fun `rejects duplicate rules`() {
    val rule = VerificationIgnoreRule(VerificationIssueCode.MISSING_READ_MODEL, "GameState")

    assertThatIllegalArgumentException()
      .isThrownBy { VerificationIgnorePolicy.apply(VerificationReport(emptyList()), listOf(rule, rule)) }
      .withMessageContaining("MISSING_READ_MODEL for 'GameState'")
  }

  private fun issue(code: VerificationIssueCode, elementId: String) = VerificationIssue(
    severity = VerificationSeverity.ERROR,
    code = code,
    message = "Problem with $elementId",
    emnElementId = elementId,
  )
}
