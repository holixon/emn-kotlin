package io.holixon.emn.generation

import io.toolisticon.kotlin.avro.AvroParser
import io.toolisticon.kotlin.avro.generator.AvroKotlinGenerator
import io.toolisticon.kotlin.avro.generator.DefaultAvroKotlinGeneratorProperties
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import java.time.Instant

object AvroKotlinFixture {

  val DEFAULT_PROPERTIES = DefaultAvroKotlinGeneratorProperties(nowSupplier = Instant::now)
  val PARSER = AvroParser()
  val DEFAULT_REGISTRY = AvroCodeGenerationSpiRegistry.load()
  val DEFAULT_GENERATOR = AvroKotlinGenerator(properties = DEFAULT_PROPERTIES, registry = DEFAULT_REGISTRY)
}
