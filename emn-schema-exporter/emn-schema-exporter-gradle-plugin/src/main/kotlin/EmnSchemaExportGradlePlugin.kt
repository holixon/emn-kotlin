package io.holixon.emn.generation.schema.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class EmnSchemaExportGradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create(
      EXTENSION_NAME,
      EmnSchemaExportExtension::class.java
    ).apply {
      outputDirectory.convention(project.layout.buildDirectory.dir("generated/resources/emn-schemas"))
      sourceRoot.convention(project.projectDir.absolutePath)
    }

    project.tasks.register(EXPORT_TASK_NAME, ExportEmnSchemasTask::class.java) { task ->
      task.group = "code generation"
      task.description = "Exports JSON Schemas and metadata from EMN files."
      task.emnFiles.from(extension.emnFiles)
      task.schemaResourceDirectories.from(extension.schemaResourceDirectories)
      task.outputDirectory.convention(extension.outputDirectory)
      task.sourceRoot.convention(extension.sourceRoot)
    }
  }

  companion object {
    const val PLUGIN_ID = "io.holixon.emn.schema-export"
    const val EXTENSION_NAME = "emnSchemaExport"
    const val EXPORT_TASK_NAME = "exportEmnSchemas"
  }
}
