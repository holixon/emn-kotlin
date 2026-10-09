package io.holixon.emn.verify.gradle

import io.holixon.emn.verify.VerificationIgnoreRule
import io.holixon.emn.verify.VerificationIssueCode
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class EmnVerifyExtension @Inject constructor(objects: ObjectFactory) {
  abstract val emnFiles: ConfigurableFileCollection
  abstract val classDirectories: ConfigurableFileCollection
  abstract val failOnWarnings: Property<Boolean>

  val ignoredIssues: EmnVerifyIgnoredIssues = objects.newInstance(EmnVerifyIgnoredIssues::class.java)

  fun ignoredIssues(action: Action<in EmnVerifyIgnoredIssues>) {
    action.execute(ignoredIssues)
  }
}

open class EmnVerifyIgnoredIssues {
  private val configuredRules = mutableListOf<VerificationIgnoreRule>()

  fun ignoreMissingReadModel(elementId: String, reason: String? = null) {
    ignore(VerificationIssueCode.MISSING_READ_MODEL, elementId, reason)
  }

  fun ignoreMissingCommandHandler(elementId: String, reason: String? = null) {
    ignore(VerificationIssueCode.MISSING_COMMAND_HANDLER, elementId, reason)
  }

  fun ignoreMissingSpecification(elementId: String, reason: String? = null) {
    ignore(VerificationIssueCode.MISSING_SPECIFICATION, elementId, reason)
  }

  fun ignore(code: VerificationIssueCode, elementId: String, reason: String? = null) {
    require(elementId.isNotBlank()) { "An EMN element ID is required for an ignored verification issue." }
    configuredRules += VerificationIgnoreRule(code, elementId, reason?.takeIf(String::isNotBlank))
  }

  internal fun rules(): List<VerificationIgnoreRule> = configuredRules.toList()
}
