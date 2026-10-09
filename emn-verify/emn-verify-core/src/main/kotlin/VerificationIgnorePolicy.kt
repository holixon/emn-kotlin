package io.holixon.emn.verify

data class VerificationIgnoreRule(
  val code: VerificationIssueCode,
  val elementId: String,
  val reason: String? = null,
)

data class IgnoredVerificationIssue(
  val issue: VerificationIssue,
  val rule: VerificationIgnoreRule,
)

data class VerificationIgnoreResult(
  val report: VerificationReport,
  val ignoredIssues: List<IgnoredVerificationIssue>,
  val unmatchedRules: List<VerificationIgnoreRule>,
)

object VerificationIgnorePolicy {

  fun apply(
    report: VerificationReport,
    rules: Collection<VerificationIgnoreRule>,
  ): VerificationIgnoreResult {
    val duplicateRules = rules.groupBy { it.code to it.elementId }.filterValues { it.size > 1 }
    require(duplicateRules.isEmpty()) {
      "Duplicate EMN verification ignore rule(s): " + duplicateRules.keys
        .sortedWith(compareBy({ it.first }, { it.second }))
        .joinToString { (code, elementId) -> "$code for '$elementId'" }
    }

    val rulesByIssue = rules.associateBy { it.code to it.elementId }
    val ignoredIssues = report.issues.mapNotNull { issue ->
      rulesByIssue[issue.code to issue.emnElementId]?.let { rule -> IgnoredVerificationIssue(issue, rule) }
    }
    val matchedRules = ignoredIssues.mapTo(mutableSetOf()) { it.rule.code to it.rule.elementId }
    val unmatchedRules = rules
      .filterNot { (it.code to it.elementId) in matchedRules }
      .sortedWith(compareBy({ it.code }, { it.elementId }))
    val ignoredIssueSet = ignoredIssues.mapTo(mutableSetOf()) { it.issue }

    return VerificationIgnoreResult(
      report = VerificationReport(report.issues.filterNot(ignoredIssueSet::contains)),
      ignoredIssues = ignoredIssues,
      unmatchedRules = unmatchedRules,
    )
  }
}
