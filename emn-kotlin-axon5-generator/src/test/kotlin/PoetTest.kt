package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import io.holixon.emn.generation.TestFixtures.logger
import io.toolisticon.kotlin.avro.generator.DefaultAvroKotlinGeneratorProperties
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.avro.value.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalKotlinPoetApi::class)
class PoetTest {

  @Test
  fun name(@TempDir tempDir: File) {
    val protocol = AVRO_PARSER.parseProtocol(resourceUrl("faculty.avpr"))
    val ctx = ProtocolDeclarationContext.of(
      declaration = protocol,
      registry = AvroCodeGenerationSpiRegistry(TestFixtures.SPI_REGISTRY),
      properties = DefaultAvroKotlinGeneratorProperties()
    )

    val type = ctx.avroTypes[Name("CreateCourse")]!! as RecordType
    val poetType = ctx.avroPoetTypes.get(type.hashCode)

    val f = type.getField(Name("courseId"))!!

    val block = initializeMessage(poetType, ctx.avroPoetTypes, mapOf(
      "courseId" to "4711",
      "name" to "Course 1",
      "capacity" to 30
    ))

    logger.info { "Block: $block" }
  }
//  {
//    "type": "record",
//    "name": "CreateCourse",
//    "namespace": "io.holixon.emn.example.faculty",
//    "fields": [
//    {
//      "name": "courseId",
//      "namespace": "io.holixon.emn.example.faculty",
//      "type": "CourseId"
//    },
//    {
//      "name": "name",
//      "type": "string"
//    },
//    {
//      "name": "capacity",
//      "type": "int"
//    }
//    ]
//  },
}
