package io.holixon.emn.gradle

import io.holixon.emn.verify.VerificationIgnoreRule
import io.holixon.emn.verify.VerificationIssueCode
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class EmnExtension @Inject constructor(objects: ObjectFactory) {
  abstract val emnFiles: ConfigurableFileCollection
  abstract val schemaResourceDirectories: ConfigurableFileCollection
  abstract val rootPackageName: Property<String>

  val kotlin: EmnKotlinExtension = objects.newInstance(EmnKotlinExtension::class.java)
  val specifications: EmnSpecificationsExtension = objects.newInstance(EmnSpecificationsExtension::class.java)
  val schemaExport: EmnSchemaExportExtension = objects.newInstance(EmnSchemaExportExtension::class.java)
  val verification: EmnVerificationExtension = objects.newInstance(EmnVerificationExtension::class.java)

  fun kotlin(action: Action<in EmnKotlinExtension>) = action.execute(kotlin)
  fun specifications(action: Action<in EmnSpecificationsExtension>) = action.execute(specifications)
  fun schemaExport(action: Action<in EmnSchemaExportExtension>) = action.execute(schemaExport)
  fun verification(action: Action<in EmnVerificationExtension>) = action.execute(verification)
}

abstract class EmnKotlinExtension {
  abstract val enabled: Property<Boolean>
  abstract val outputDirectory: DirectoryProperty
  abstract val annotationEnhancers: ListProperty<String>
}

abstract class EmnSpecificationsExtension {
  abstract val enabled: Property<Boolean>
  abstract val outputDirectory: DirectoryProperty
  abstract val fixtureProvider: Property<String>
  abstract val errorVerifier: Property<String>
}

abstract class EmnSchemaExportExtension {
  abstract val enabled: Property<Boolean>
  abstract val outputDirectory: DirectoryProperty
  abstract val sourceRoot: Property<String>
}

abstract class EmnVerificationExtension @Inject constructor(objects: ObjectFactory) {
  abstract val enabled: Property<Boolean>
  abstract val classDirectories: ConfigurableFileCollection
  abstract val failOnWarnings: Property<Boolean>

  val ignoredIssues: EmnIgnoredIssues = objects.newInstance(EmnIgnoredIssues::class.java)

  fun ignoredIssues(action: Action<in EmnIgnoredIssues>) = action.execute(ignoredIssues)
}

open class EmnIgnoredIssues {
  private val configuredRules = mutableListOf<VerificationIgnoreRule>()

  fun ignoreMissingReadModel(elementId: String, reason: String? = null) =
    ignore(VerificationIssueCode.MISSING_READ_MODEL, elementId, reason)

  fun ignoreMissingCommandHandler(elementId: String, reason: String? = null) =
    ignore(VerificationIssueCode.MISSING_COMMAND_HANDLER, elementId, reason)

  fun ignoreMissingSpecification(elementId: String, reason: String? = null) =
    ignore(VerificationIssueCode.MISSING_SPECIFICATION, elementId, reason)

  fun ignore(code: VerificationIssueCode, elementId: String, reason: String? = null) {
    require(elementId.isNotBlank()) { "An EMN element ID is required for an ignored verification issue." }
    configuredRules += VerificationIgnoreRule(code, elementId, reason?.takeIf(String::isNotBlank))
  }

  internal fun rules(): List<VerificationIgnoreRule> = configuredRules.toList()
}
