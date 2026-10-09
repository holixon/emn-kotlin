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
    }
    val generateTask = project.tasks.register(GENERATE_TASK_NAME, GenerateEmnSchemaKotlinTask::class.java) { task ->
      task.group = "code generation"
      task.description = "Generates Kotlin classes from JSON Schemas embedded in EMN files."
      task.emnFiles.from(extension.emnFiles)
      task.outputDirectory.convention(extension.outputDirectory)
      task.rootPackageName.convention(extension.rootPackageName)
      task.annotationEnhancers.convention(extension.annotationEnhancers)
    }

    project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      registerGeneratedKotlinSources(project, generateTask)
    }
  }

  private fun registerGeneratedKotlinSources(
    project: Project,
    generateTask: TaskProvider<GenerateEmnSchemaKotlinTask>
  ) {
    val kotlinExtension = project.extensions.getByName("kotlin")
    val sourceSets = kotlinExtension.javaClass.methods
      .firstOrNull { it.name == "getSourceSets" && it.parameterCount == 0 }
      ?.invoke(kotlinExtension) as? NamedDomainObjectContainer<*>
      ?: throw GradleException("The applied Kotlin JVM plugin does not expose Kotlin source sets.")
    val mainSourceSet = sourceSets.getByName("main")
    val generatedKotlin = mainSourceSet.javaClass.methods
      .firstOrNull { it.name == "getGeneratedKotlin" && it.parameterCount == 0 }
      ?.invoke(mainSourceSet) as? SourceDirectorySet
      ?: throw GradleException(
        "This plugin requires a Kotlin Gradle Plugin version that exposes KotlinSourceSet.generatedKotlin."
      )
    generatedKotlin.srcDir(generateTask)
  }

  companion object {
    const val PLUGIN_ID = "io.holixon.emn.kotlin-schema"
    const val EXTENSION_NAME = "emnSchemaKotlin"
    const val GENERATE_TASK_NAME = "generateEmnSchemaKotlin"
  }
}
