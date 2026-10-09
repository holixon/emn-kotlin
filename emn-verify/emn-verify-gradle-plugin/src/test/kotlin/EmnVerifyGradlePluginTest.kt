package io.holixon.emn.verify.gradle

import io.holixon.emn.verify.VerificationIssueCode
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class EmnVerifyGradlePluginTest {

  @Test
  fun `registers verification extension and verification task`() {
    val project = ProjectBuilder.builder().build()

    project.tasks.register("classes")
    project.tasks.register("testClasses")
    project.pluginManager.apply(EmnVerifyGradlePlugin.PLUGIN_ID)

    val extension = project.extensions.getByType(EmnVerifyExtension::class.java)
    val task = project.tasks.named(
      EmnVerifyGradlePlugin.VERIFY_TASK_NAME,
      VerifyEmnTask::class.java,
    ).get()
    assertThat(extension.failOnWarnings.get()).isFalse()
    assertThat(task.failOnWarnings.get()).isFalse()
    assertThat(task.classDirectories.files.map { it.path }).anyMatch { it.endsWith("classes/kotlin/main") }
    assertThat(task.taskDependencies.getDependencies(task).map { it.name })
      .contains("classes", "testClasses")
  }

  @Test
  fun `configures ignored verification issues through convenience and generic methods`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(EmnVerifyGradlePlugin.PLUGIN_ID)
    val extension = project.extensions.getByType(EmnVerifyExtension::class.java)

    extension.ignoredIssues {
      it.ignoreMissingReadModel("GameState", "Implemented by the frontend")
      it.ignoreMissingCommandHandler("CloseGameCommand")
      it.ignore(VerificationIssueCode.MISSING_SPECIFICATION, "Specification_123")
    }

    val task = project.tasks.named(
      EmnVerifyGradlePlugin.VERIFY_TASK_NAME,
      VerifyEmnTask::class.java,
    ).get()
    assertThat(task.ignoredIssueCodes.get()).containsExactly(
      "MISSING_READ_MODEL",
      "MISSING_COMMAND_HANDLER",
      "MISSING_SPECIFICATION",
    )
    assertThat(task.ignoredIssueElementIds.get()).containsExactly(
      "GameState",
      "CloseGameCommand",
      "Specification_123",
    )
    assertThat(task.ignoredIssueReasons.get()).containsExactly(
      "Implemented by the frontend",
      "",
      "",
    )
  }
}
