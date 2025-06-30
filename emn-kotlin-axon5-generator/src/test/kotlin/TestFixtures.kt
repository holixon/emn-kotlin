package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.toolisticon.kotlin.avro.AvroParser
import io.toolisticon.kotlin.avro.generator.DefaultAvroKotlinGeneratorProperties
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import java.time.Instant

@OptIn(ExperimentalKotlinPoetApi::class)
object TestFixtures {
  // loads ALL available strategies and processors for ALL contexts
  val SPI_REGISTRY = load()

  object AvroKotlinFixtures {

    val AVRO_DEFAULT_PROPERTIES = DefaultAvroKotlinGeneratorProperties(nowSupplier = Instant::now)

    val AVRO_PARSER = AvroParser()

  }

}
