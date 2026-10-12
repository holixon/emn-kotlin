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
    val specificationTask = project.tasks.named(
      EmnSchemaKotlinGradlePlugin.GENERATE_SPECIFICATION_TESTS_TASK_NAME,
      GenerateEmnSpecificationTestsTask::class.java
    ).get()
    assertThat(extension.outputDirectory.get().asFile)
      .isEqualTo(project.layout.buildDirectory.dir("generated/sources/emn-schema-kotlin").get().asFile)
    assertThat(extension.annotationEnhancers.get()).isEmpty()
    assertThat(extension.specifications.generate.get()).isFalse()
    assertThat(task.outputDirectory.get()).isEqualTo(extension.outputDirectory.get())
    assertThat(specificationTask.outputDirectory.get()).isEqualTo(extension.specifications.outputDirectory.get())
  }

  @Test
  fun `configures specification generation through nested extension`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(EmnSchemaKotlinGradlePlugin.PLUGIN_ID)
    val extension = project.extensions.getByType(EmnSchemaKotlinExtension::class.java)

    extension.specifications {
      generate.set(true)
      fixtureProvider.set("org.quizmania.test.QuizmaniaFixtureProvider")
      errorVerifier.set("org.quizmania.test.QuizmaniaErrorVerifier")
    }

    val task = project.tasks.named(
      EmnSchemaKotlinGradlePlugin.GENERATE_SPECIFICATION_TESTS_TASK_NAME,
      GenerateEmnSpecificationTestsTask::class.java
    ).get()
    assertThat(task.generationEnabled.get()).isTrue()
    assertThat(task.fixtureProvider.get()).isEqualTo("org.quizmania.test.QuizmaniaFixtureProvider")
    assertThat(task.errorVerifier.get()).isEqualTo("org.quizmania.test.QuizmaniaErrorVerifier")
  }
}
