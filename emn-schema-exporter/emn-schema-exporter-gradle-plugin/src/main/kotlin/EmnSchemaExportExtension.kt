package io.holixon.emn.generation.schema.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class EmnSchemaExportExtension {
  abstract val emnFiles: ConfigurableFileCollection
  abstract val schemaResourceDirectories: ConfigurableFileCollection
  abstract val outputDirectory: DirectoryProperty
  abstract val sourceRoot: Property<String>
}
