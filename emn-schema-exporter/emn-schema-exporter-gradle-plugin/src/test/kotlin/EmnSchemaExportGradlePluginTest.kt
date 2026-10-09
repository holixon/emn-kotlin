package io.holixon.emn.generation.schema.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class EmnSchemaExportGradlePluginTest {

  @Test
  fun `registers schema export extension and task`() {
    val project = ProjectBuilder.builder().build()

    project.pluginManager.apply(EmnSchemaExportGradlePlugin.PLUGIN_ID)

    val extension = project.extensions.getByType(EmnSchemaExportExtension::class.java)
    val task = project.tasks.named(
      EmnSchemaExportGradlePlugin.EXPORT_TASK_NAME,
      ExportEmnSchemasTask::class.java
    ).get()
    assertThat(extension.outputDirectory.get().asFile)
      .isEqualTo(project.layout.buildDirectory.dir("generated/resources/emn-schemas").get().asFile)
    assertThat(extension.sourceRoot.get()).isEqualTo(project.projectDir.absolutePath)
    assertThat(task.outputDirectory.get()).isEqualTo(extension.outputDirectory.get())
    assertThat(task.sourceRoot.get()).isEqualTo(extension.sourceRoot.get())
  }
}
