package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import io.holixon.emn.generation.TestFixtures.logger
import io.toolisticon.kotlin.avro.generator.DefaultAvroKotlinGeneratorProperties
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.avro.value.Name
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

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
  @Test
  fun createsCreateCourseInstantiation() {
    val protocol = AVRO_PARSER.parseProtocol(resourceUrl("faculty/faculty.avpr"))
    val ctx = ProtocolDeclarationContext.of(
      declaration = protocol,
      registry = AvroCodeGenerationSpiRegistry(TestFixtures.SPI_REGISTRY),
      properties = DefaultAvroKotlinGeneratorProperties()
    )

    val courseIdType = ctx.avroTypes[Name("CourseId")]!! as RecordType
    val createCourseType = ctx.avroTypes[Name("CreateCourse")]!! as RecordType
    val createCoursePoetType = ctx.avroPoetTypes[createCourseType.hashCode]
    val courseIdPoetType = ctx.avroPoetTypes[courseIdType.hashCode]

    val block = initializeMessage(
      createCoursePoetType, ctx.avroPoetTypes, mapOf(
        "courseId" to "4711",
        "name" to "Course 1",
        "capacity" to 30,
      )
    )
    logger.info { "Block: $block" }

    assertThat(block.formatParts()).containsExactly(
      "%T", "(",
      "courseId = ", "%L", ", ",
      "name = ", "%S", ", ",
      "capacity = ", "%L",
      ")"
    )
    val args = block.args()
    assertThat(args).isNotNull.isNotEmpty.hasSize(4)
    assertThat(args).contains(
      createCoursePoetType.typeName,
      "Course 1",
      "30"
    )
    assertThat(args!![1]).isInstanceOf(CodeBlock::class.java)
    val complexConstructorCode: CodeBlock = args[1] as CodeBlock
    assertThat(complexConstructorCode.formatParts()).containsExactly("%T", "(", "%S", ")")
    assertThat(complexConstructorCode.args()).containsExactly(
      courseIdPoetType.typeName,
      "4711"
    )
  }


  fun CodeBlock.formatParts() = getField<List<String>>(this, "formatParts")
  fun CodeBlock.args() = getField<List<Any>>(this, "args")

  @Suppress("UNCHECKED_CAST")
  fun <T> getField(instance: Any, fieldName: String): T? {
    val field: Field = instance.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(instance) as? T
  }
}

