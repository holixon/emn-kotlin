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
}

/**
 * Default implementation of the properties.
 */
data class DefaultEmnAxon5GeneratorProperties(
    override val emnName: String,
    override val rootPackageName: PackageName,
    override val schemaTypeSuffix: String = "",
    override val suppressRedundantModifiers: Boolean = true,
    override val nowSupplier: () -> Instant = { Instant.now() }
) : EmnAxon5GeneratorProperties
