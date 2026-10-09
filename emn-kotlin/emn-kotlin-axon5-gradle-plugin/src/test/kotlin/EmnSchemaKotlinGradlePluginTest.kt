package io.holixon.emn.generation.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class EmnSchemaKotlinGradlePluginTest {

  @Test
  fun `registers generation extension and task`() {
    val project = ProjectBuilder.builder().build()

    project.pluginManager.apply(EmnSchemaKotlinGradlePlugin.PLUGIN_ID)

    val extension = project.extensions.getByType(EmnSchemaKotlinExtension::class.java)
    val task = project.tasks.named(
      EmnSchemaKotlinGradlePlugin.GENERATE_TASK_NAME,
      GenerateEmnSchemaKotlinTask::class.java
    ).get()
    assertThat(extension.outputDirectory.get().asFile)
      .isEqualTo(project.layout.buildDirectory.dir("generated/sources/emn-schema-kotlin").get().asFile)
    assertThat(extension.annotationEnhancers.get()).isEmpty()
    assertThat(task.outputDirectory.get()).isEqualTo(extension.outputDirectory.get())
  }
}
