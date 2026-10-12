package io.holixon.emn.gradle

import io.holixon.emn.verify.VerificationIssueCode
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class EmnGradlePluginTest {

  @Test
  fun `registers one extension and all public tasks`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("java")

    project.pluginManager.apply(EmnGradlePlugin.PLUGIN_ID)

    val extension = project.extensions.getByType(EmnExtension::class.java)
    assertThat(project.extensions.findByName("emnSchemaKotlin")).isNull()
    assertThat(project.extensions.findByName("emnSchemaExport")).isNull()
    assertThat(project.extensions.findByName("emnVerify")).isNull()
    assertThat(extension.kotlin.enabled.get()).isFalse()
    assertThat(extension.specifications.enabled.get()).isFalse()
    assertThat(extension.schemaExport.enabled.get()).isFalse()
    assertThat(extension.verification.enabled.get()).isFalse()
    assertThat(project.tasks.names).contains(
      EmnGradlePlugin.GENERATE_KOTLIN_TASK_NAME,
      EmnGradlePlugin.GENERATE_SPECIFICATIONS_TASK_NAME,
      EmnGradlePlugin.EXPORT_SCHEMAS_TASK_NAME,
      EmnGradlePlugin.VERIFY_TASK_NAME,
    )
    val processResources = project.tasks.named("processResources").get()
    assertThat(processResources.taskDependencies.getDependencies(processResources).map { it.name })
      .contains(EmnGradlePlugin.EXPORT_SCHEMAS_TASK_NAME)
  }

  @Test
  fun `maps unified verification policy to verification task`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(EmnGradlePlugin.PLUGIN_ID)
    val extension = project.extensions.getByType(EmnExtension::class.java)

    extension.verification.ignoredIssues {
      it.ignoreMissingReadModel("GameState", "Implemented by the frontend")
      it.ignore(VerificationIssueCode.MISSING_COMMAND_HANDLER, "CloseGameCommand")
    }

    val task = project.tasks.named(EmnGradlePlugin.VERIFY_TASK_NAME).get() as VerifyEmnTask
    assertThat(task.ignoredIssueCodes.get()).containsExactly("MISSING_READ_MODEL", "MISSING_COMMAND_HANDLER")
    assertThat(task.ignoredIssueElementIds.get()).containsExactly("GameState", "CloseGameCommand")
  }
}
