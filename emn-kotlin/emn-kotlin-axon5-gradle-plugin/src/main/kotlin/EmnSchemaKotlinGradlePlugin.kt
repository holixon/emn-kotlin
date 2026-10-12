package io.holixon.emn.generation.gradle

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider

class EmnSchemaKotlinGradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create(EXTENSION_NAME, EmnSchemaKotlinExtension::class.java).apply {
      outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/emn-schema-kotlin"))
      annotationEnhancers.convention(emptyList())
      specifications.apply {
        generate.convention(false)
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/emn-specification-tests"))
      }
    }
    val generateTask = project.tasks.register(GENERATE_TASK_NAME, GenerateEmnSchemaKotlinTask::class.java) { task ->
      task.group = "code generation"
      task.description = "Generates Kotlin classes from JSON Schemas embedded in EMN files."
      task.emnFiles.from(extension.emnFiles)
      task.outputDirectory.convention(extension.outputDirectory)
      task.rootPackageName.convention(extension.rootPackageName)
      task.annotationEnhancers.convention(extension.annotationEnhancers)
    }
    val generateSpecificationsTask = project.tasks.register(
      GENERATE_SPECIFICATION_TESTS_TASK_NAME,
      GenerateEmnSpecificationTestsTask::class.java
    ) { task ->
      task.group = "verification"
      task.description = "Generates Axon 5 fixture tests from EMN specifications."
      task.emnFiles.from(extension.emnFiles)
      task.outputDirectory.convention(extension.specifications.outputDirectory)
      task.rootPackageName.convention(extension.rootPackageName)
      task.generationEnabled.convention(extension.specifications.generate)
      task.fixtureProvider.convention(extension.specifications.fixtureProvider)
      task.errorVerifier.convention(extension.specifications.errorVerifier)
    }

    project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      registerGeneratedKotlinSources(project, "main", generateTask)
      registerGeneratedKotlinSources(project, "test", generateSpecificationsTask)
    }
  }

  private fun registerGeneratedKotlinSources(
    project: Project,
    sourceSetName: String,
    generateTask: TaskProvider<out org.gradle.api.Task>
  ) {
    val kotlinExtension = project.extensions.getByName("kotlin")
    val sourceSets = kotlinExtension.javaClass.methods
      .firstOrNull { it.name == "getSourceSets" && it.parameterCount == 0 }
      ?.invoke(kotlinExtension) as? NamedDomainObjectContainer<*>
      ?: throw GradleException("The applied Kotlin JVM plugin does not expose Kotlin source sets.")
    val sourceSet = sourceSets.getByName(sourceSetName)
    val generatedKotlin = sourceSet.javaClass.methods
      .firstOrNull { it.name == "getGeneratedKotlin" && it.parameterCount == 0 }
      ?.invoke(sourceSet) as? SourceDirectorySet
      ?: throw GradleException(
        "This plugin requires a Kotlin Gradle Plugin version that exposes KotlinSourceSet.generatedKotlin."
      )
    generatedKotlin.srcDir(generateTask)
  }

  companion object {
    const val PLUGIN_ID = "io.holixon.emn.kotlin-schema"
    const val EXTENSION_NAME = "emnSchemaKotlin"
    const val GENERATE_TASK_NAME = "generateEmnSchemaKotlin"
    const val GENERATE_SPECIFICATION_TESTS_TASK_NAME = "generateEmnSpecificationTests"
  }
}
