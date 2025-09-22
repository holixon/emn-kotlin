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

@OptIn(ExperimentalKotlinPoetApi::class)
class PoetTest {

  /**
   * //  {
   * //    "type": "record",
   * //    "name": "CreateCourse",
   * //    "namespace": "io.holixon.emn.example.faculty",
   * //    "fields": [
   * //    {
   * //      "name": "courseId",
   * //      "namespace": "io.holixon.emn.example.faculty",
   * //      "type": "CourseId"
   * //    },
   * //    {
   * //      "name": "name",
   * //      "type": "string"
   * //    },
   * //    {
   * //      "name": "capacity",
   * //      "type": "int"
   * //    }
   * //    ]
   * //  },
   *
   */
  @Deprecated("This is a spike, not a test, should be removed or converted to a real test.")
  @Test
  fun createsCreateCourseInstantiation() {
    val protocol = AVRO_PARSER.parseProtocol(resourceUrl("faculty/faculty.avpr"))
    val ctx = ProtocolDeclarationContext.of(
      declaration = protocol,
      registry = AvroCodeGenerationSpiRegistry(TestFixtures.SPI_REGISTRY),
      properties = DefaultAvroKotlinGeneratorProperties()
    )

    val type = ctx.avroTypes[Name("CreateCourse")]!! as RecordType
    val poetType = ctx.avroPoetTypes[type.hashCode]

    val block = initializeMessage(poetType, ctx.avroPoetTypes, mapOf(
      "courseId" to "4711",
      "name" to "Course 1",
      "capacity" to 30,
    ))

    logger.info { "Block: $block" }
  }

}
