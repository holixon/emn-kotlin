package io.holixon.emn.generation.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class EmnSchemaKotlinExtension @Inject constructor(objects: ObjectFactory) {
  abstract val emnFiles: ConfigurableFileCollection
  abstract val outputDirectory: DirectoryProperty
  abstract val rootPackageName: Property<String>
  abstract val annotationEnhancers: ListProperty<String>

  val specifications: EmnSpecificationsExtension = objects.newInstance(EmnSpecificationsExtension::class.java)

  fun specifications(action: EmnSpecificationsExtension.() -> Unit) {
    specifications.action()
  }
}

abstract class EmnSpecificationsExtension {
  abstract val generate: Property<Boolean>
  abstract val fixtureProvider: Property<String>
  abstract val errorVerifier: Property<String>
  abstract val outputDirectory: DirectoryProperty
}
