package io.holixon.emn.maven

import io.holixon.emn.verify.VerificationIgnorePolicy
import io.holixon.emn.verify.VerificationIgnoreRule
import io.holixon.emn.verify.VerificationIssueCode
import io.holixon.emn.verify.VerificationSeverity
import io.holixon.emn.verify.axon5.Axon5EmnVerificationRequest
import io.holixon.emn.verify.axon5.Axon5EmnVerificationService
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, requiresProject = true)
class VerifyMojo : AbstractEmnMojo() {

  @Parameter(property = "emn.verification.classesDirectory", defaultValue = $$"${project.build.outputDirectory}")
  private lateinit var classesDirectory: File

  @Parameter(property = "emn.verification.testClassesDirectory", defaultValue = $$"${project.build.testOutputDirectory}")
  private lateinit var testClassesDirectory: File

  @Parameter(property = "emn.verification.failOnWarnings", defaultValue = "false")
  private var failOnWarnings: Boolean = false

  @Parameter(property = "emn.verification.ignoredIssues")
  private var ignoredIssues: Array<IgnoredIssue> = emptyArray()

  override fun execute() {
    val emnFiles = findEmnFiles(failWhenEmpty = true)
    val result = Axon5EmnVerificationService().evaluate(Axon5EmnVerificationRequest(
      emnFiles = emnFiles,
      classDirectories = listOf(classesDirectory, testClassesDirectory),
    ))
    val ignoreResult = try {
      VerificationIgnorePolicy.apply(result.report, ignoredIssues.map(IgnoredIssue::toRule))
    } catch (exception: IllegalArgumentException) {
      throw MojoFailureException(exception.message, exception)
    }

    result.summaryLines().forEach(log::info)
    if (ignoreResult.ignoredIssues.isNotEmpty()) {
      log.info("EMN verification policy ignored ${ignoreResult.ignoredIssues.size} issue(s).")
    }
    result.debugLines().forEach(log::debug)
    ignoreResult.ignoredIssues.forEach { ignored ->
      val reason = ignored.rule.reason?.let { " Reason: $it" }.orEmpty()
      log.debug("IGNORED ${ignored.issue.code} for '${ignored.rule.elementId}': ${ignored.issue.message}$reason")
    }
    ignoreResult.unmatchedRules.forEach { rule ->
      log.warn("EMN verification ignore rule ${rule.code} for '${rule.elementId}' matched no issue.")
    }
    val report = ignoreResult.report
    report.issues.forEach { issue ->
      val message = "${issue.code}: ${issue.message}"
      when (issue.severity) {
        VerificationSeverity.ERROR -> log.error(message)
        VerificationSeverity.WARNING -> log.warn(message)
      }
    }
    if (report.errors.isNotEmpty() || (failOnWarnings && report.warnings.isNotEmpty())) {
      throw MojoFailureException(
        "EMN verification failed with ${report.errors.size} error(s) and ${report.warnings.size} warning(s)."
      )
    }
    log.info("EMN verification passed with ${report.warnings.size} warning(s).")
  }
}

class IgnoredIssue {
  lateinit var code: String
  lateinit var elementId: String
  var reason: String? = null

  fun toRule(): VerificationIgnoreRule {
    val issueCode = try {
      VerificationIssueCode.valueOf(code.trim().uppercase())
    } catch (exception: IllegalArgumentException) {
      throw IllegalArgumentException("Unknown EMN verification issue code '$code'.", exception)
    }
    return VerificationIgnoreRule(issueCode, elementId, reason?.takeIf(String::isNotBlank))
  }
}
