package io.holixon.emn.generation

import io.toolisticon.kotlin.avro.generator.AvroKotlinGeneratorProperties
import io.toolisticon.kotlin.generation.PackageName
import java.time.Instant

/**
 * Properties for EMN generator.
 */
interface EmnAxon5GeneratorProperties : AvroKotlinGeneratorProperties {

  val emnName: String

  /**
   * Package name for generation.
   */
  val rootPackageName: PackageName

  /**
   * Package name for the command slice generation.
   */
  val commandSliceRootPackageName: PackageName get() = "$rootPackageName.write"

  /**
   * Should command handlers based on command slices be generated
   */
  val generateCommandSlices: Boolean
  /**
   * Should command slice tests based on Axon test fixtures be generated
   */
  val generateCommandSliceTests: Boolean

  val generateConcreteStateImpl: Boolean

  /**
   * If true, the instancio wi used for object creation, allowing to specify only relevant values.
   */
  val instanceCreator: String
}

/**
 * Default implementation of the properties.
 */
data class DefaultEmnAxon5GeneratorProperties(
  override val emnName: String,
  override val rootPackageName: PackageName,
  override val schemaTypeSuffix: String = "",
  override val suppressRedundantModifiers: Boolean = true,
  override val nowSupplier: () -> Instant = { Instant.now() },
  override val generateCommandSlices: Boolean = true,
  override val generateCommandSliceTests: Boolean = true,
  override val generateConcreteStateImpl: Boolean = true,
  override val instanceCreator: String = "none"
) : EmnAxon5GeneratorProperties
