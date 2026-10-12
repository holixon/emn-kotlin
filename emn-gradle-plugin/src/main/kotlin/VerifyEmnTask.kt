package io.holixon.emn.gradle

import io.holixon.emn.verify.VerificationIgnorePolicy
import io.holixon.emn.verify.VerificationIgnoreRule
import io.holixon.emn.verify.VerificationIssueCode
import io.holixon.emn.verify.VerificationSeverity
import io.holixon.emn.verify.axon5.Axon5EmnVerificationRequest
import io.holixon.emn.verify.axon5.Axon5EmnVerificationService
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyEmnTask : DefaultTask() {

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val emnFiles: ConfigurableFileCollection

  @get:Classpath
  abstract val classDirectories: ConfigurableFileCollection

  @get:Input
  abstract val failOnWarnings: Property<Boolean>

  @get:Input
  abstract val ignoredIssueCodes: ListProperty<String>

  @get:Input
  abstract val ignoredIssueElementIds: ListProperty<String>

  @get:Input
  abstract val ignoredIssueReasons: ListProperty<String>

  @TaskAction
  fun verify() {
    if (emnFiles.isEmpty) {
      throw GradleException("No EMN files configured for verifyEmn. Configure emn.emnFiles.")
    }
    val missingEmnFiles = emnFiles.files.filterNot { it.isFile }
    if (missingEmnFiles.isNotEmpty()) {
      throw GradleException(
        "Configured EMN file(s) do not exist: ${missingEmnFiles.joinToString { it.path }}"
      )
    }
    val result = Axon5EmnVerificationService().evaluate(Axon5EmnVerificationRequest(
      emnFiles = emnFiles.files,
      classDirectories = classDirectories.files,
    ))
    val ignoreResult = try {
      VerificationIgnorePolicy.apply(result.report, ignoreRules())
    } catch (exception: IllegalArgumentException) {
      throw GradleException(exception.message ?: "Invalid EMN verification ignore configuration.", exception)
    }

    result.summaryLines().forEach(logger::lifecycle)
    if (ignoreResult.ignoredIssues.isNotEmpty()) {
      logger.lifecycle("EMN verification policy ignored ${ignoreResult.ignoredIssues.size} issue(s).")
    }
    result.debugLines().forEach(logger::debug)
    ignoreResult.ignoredIssues.forEach { ignored ->
      val reason = ignored.rule.reason?.let { " Reason: $it" }.orEmpty()
      logger.debug("IGNORED ${ignored.issue.code} for '${ignored.rule.elementId}': ${ignored.issue.message}$reason")
    }
    ignoreResult.unmatchedRules.forEach { rule ->
      logger.warn("EMN verification ignore rule ${rule.code} for '${rule.elementId}' matched no issue.")
    }
    val report = ignoreResult.report
    report.issues.forEach { issue ->
      val message = "${issue.code}: ${issue.message}"
      when (issue.severity) {
        VerificationSeverity.ERROR -> logger.error(message)
        VerificationSeverity.WARNING -> logger.warn(message)
      }
    }
    if (report.errors.isNotEmpty() || (failOnWarnings.get() && report.warnings.isNotEmpty())) {
      throw GradleException(
        "EMN verification failed with ${report.errors.size} error(s) and ${report.warnings.size} warning(s)."
      )
    }
    logger.lifecycle("EMN verification passed with ${report.warnings.size} warning(s).")
  }

  private fun ignoreRules(): List<VerificationIgnoreRule> {
    val codes = ignoredIssueCodes.get()
    val elementIds = ignoredIssueElementIds.get()
    val reasons = ignoredIssueReasons.get()
    if (codes.size != elementIds.size || codes.size != reasons.size) {
      throw GradleException("Invalid EMN verification ignore configuration.")
    }
    return codes.indices.map { index ->
      VerificationIgnoreRule(
        code = VerificationIssueCode.valueOf(codes[index]),
        elementId = elementIds[index],
        reason = reasons[index].ifBlank { null },
      )
    }
  }
}
