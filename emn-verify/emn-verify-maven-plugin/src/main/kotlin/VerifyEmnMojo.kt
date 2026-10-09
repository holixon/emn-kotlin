package io.holixon.emn.verify.maven

import io.holixon.emn.verify.VerificationIssue
import io.holixon.emn.verify.VerificationSeverity
import io.holixon.emn.verify.axon5.Axon5EmnVerificationRequest
import io.holixon.emn.verify.axon5.Axon5EmnVerificationService
import io.toolisticon.maven.mojo.AbstractContextAwareMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import java.io.File

@Mojo(
  name = VerifyEmnMojo.GOAL,
  defaultPhase = LifecyclePhase.VERIFY,
  requiresDependencyResolution = ResolutionScope.TEST,
  requiresProject = true,
)
class VerifyEmnMojo : AbstractContextAwareMojo() {

  companion object {
    const val GOAL = "verify-emn"
  }

  @Parameter(
    property = "resourceDirectory",
    required = true,
    defaultValue = EmnVerifyMavenPlugin.DEFAULT_RESOURCES_DIRECTORY,
  )
  private lateinit var resourceDirectory: File

  @Parameter(property = "includes")
  private var includes: Array<String> = EmnVerifyMavenPlugin.DEFAULT_INCLUDES

  @Parameter(property = "classesDirectory", defaultValue = $$"${project.build.outputDirectory}")
  private lateinit var classesDirectory: File

  @Parameter(property = "testClassesDirectory", defaultValue = $$"${project.build.testOutputDirectory}")
  private lateinit var testClassesDirectory: File

  @Parameter(property = "failOnWarnings", defaultValue = "false")
  private var failOnWarnings: Boolean = false

  override fun execute() {
    if (!resourceDirectory.exists()) {
      log.warn("Skip non existing resource directory $resourceDirectory.")
      return
    }
    val emnFiles = EmnVerifyMavenPlugin.findIncludedFiles(
      absPath = resourceDirectory.absolutePath,
      includes = includes,
    ).map { File(resourceDirectory, it) }
    if (emnFiles.isEmpty()) {
      log.warn("No EMN files found in $resourceDirectory.")
      return
    }

    val result = Axon5EmnVerificationService().evaluate(Axon5EmnVerificationRequest(
      emnFiles = emnFiles,
      classDirectories = listOf(classesDirectory, testClassesDirectory),
    ))
    result.summaryLines().forEach(log::info)
    result.debugLines().forEach(log::debug)
    val report = result.report
    report.issues.forEach(::logIssue)
    if (report.errors.isNotEmpty() || (failOnWarnings && report.warnings.isNotEmpty())) {
      throw MojoFailureException(
        "EMN verification failed with ${report.errors.size} error(s) and ${report.warnings.size} warning(s)."
      )
    }
    log.info("EMN verification passed with ${report.warnings.size} warning(s).")
  }

  private fun logIssue(issue: VerificationIssue) {
    val message = "${issue.code}: ${issue.message}"
    when (issue.severity) {
      VerificationSeverity.ERROR -> log.error(message)
      VerificationSeverity.WARNING -> log.warn(message)
    }
  }
}
