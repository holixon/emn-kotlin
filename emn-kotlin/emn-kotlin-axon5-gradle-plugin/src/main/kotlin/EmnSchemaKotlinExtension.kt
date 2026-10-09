package io.holixon.emn.generation.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class EmnSchemaKotlinExtension {
  abstract val emnFiles: ConfigurableFileCollection
  abstract val outputDirectory: DirectoryProperty
  abstract val rootPackageName: Property<String>
  abstract val annotationEnhancers: ListProperty<String>
}
