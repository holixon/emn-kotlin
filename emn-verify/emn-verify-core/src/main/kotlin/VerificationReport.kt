package io.holixon.emn.verify

enum class VerificationSeverity {
  ERROR,
  WARNING,
}

enum class VerificationIssueCode {
  MISSING_COMMAND_HANDLER,
  DUPLICATE_COMMAND_HANDLER,
  MISSING_READ_MODEL,
  MISSING_EVENT_HANDLER,
  UNMODELED_EVENT_HANDLER,
  UNKNOWN_QUERY_TYPE,
  MISSING_SPECIFICATION,
  DUPLICATE_SPECIFICATION,
  UNKNOWN_SPECIFICATION,
}

data class VerificationIssue(
  val severity: VerificationSeverity,
  val code: VerificationIssueCode,
  val message: String,
  val emnElementId: String? = null,
  val codeLocation: String? = null,
)

data class VerificationReport(val issues: List<VerificationIssue>) {
  val errors: List<VerificationIssue> = issues.filter { it.severity == VerificationSeverity.ERROR }
  val warnings: List<VerificationIssue> = issues.filter { it.severity == VerificationSeverity.WARNING }
  val isSuccessful: Boolean = errors.isEmpty()

  fun requireSuccessful(failOnWarnings: Boolean = false) {
    if (errors.isNotEmpty() || (failOnWarnings && warnings.isNotEmpty())) {
      throw EmnVerificationException(this, failOnWarnings)
    }
  }
}

class EmnVerificationException(
  val report: VerificationReport,
  failOnWarnings: Boolean,
) : IllegalStateException(
  buildString {
    append("EMN verification failed")
    if (failOnWarnings && report.errors.isEmpty()) append(" because warnings are configured to fail")
    append(":\n")
    report.issues.forEach { issue ->
      append("- ${issue.severity} ${issue.code}: ${issue.message}\n")
    }
  }.trimEnd()
)
