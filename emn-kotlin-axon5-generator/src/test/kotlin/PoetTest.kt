package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Field

@OptIn(ExperimentalKotlinPoetApi::class)
class PoetTest {

  val protocol = AVRO_PARSER.parseProtocol(resourceUrl("faculty/faculty.avpr"))
  val ctx = ProtocolDeclarationContext.of(
    declaration = protocol,
    registry = AvroCodeGenerationSpiRegistry(TestFixtures.SPI_REGISTRY),
    properties = DefaultAvroKotlinGeneratorProperties()
  )


  @Test
  fun createsCreateCourseInstantiation() {

    val courseIdType = ctx.avroTypes[Name("CourseId")]!! as RecordType
    val createCourseType = ctx.avroTypes[Name("CreateCourse")]!! as RecordType
    val createCoursePoetType = ctx.avroPoetTypes[createCourseType.hashCode]
    val courseIdPoetType = ctx.avroPoetTypes[courseIdType.hashCode]

    val block = initializeMessage(
      createCoursePoetType, ctx.avroPoetTypes, jsonOf(
        """{
        "courseId": "4711",
        "name": "Course 1",
        "capacity": 30
        }
        """.trimIndent()
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

  @Test
  fun createsEnrollStudentToCourseAlternativeInstantiation() {

    val enrollType = ctx.avroTypes[Name("EnrollStudentToCourseAlternative")]!! as RecordType
    val enrollPoetType = ctx.avroPoetTypes[enrollType.hashCode]
    val courseAndStudentIdType = ctx.avroTypes[Name("CourseAndStudentCompositeKey")]!! as RecordType
    val courseAndStudentIdPoetType = ctx.avroPoetTypes[courseAndStudentIdType.hashCode]

    val block = initializeMessage(
      enrollPoetType, ctx.avroPoetTypes, jsonOf(
        """
        {
          "courseAndStudentId": {
            "courseId": "4711",
            "studentId": "0815"
          }
        }
        """.trimIndent()
      )
    )

    logger.info { "Block: $block" }

    assertThat(block.formatParts()).containsExactly(
      "%T", "(",
      "courseAndStudentId = ", "%L",
      ")"
    )
    val args = block.args()
    assertThat(args).isNotNull.isNotEmpty.hasSize(2)
    assertThat(args).contains(
      enrollPoetType.typeName,
    )
    assertThat(args!![1]).isInstanceOf(CodeBlock::class.java)
    val complexConstructorCode: CodeBlock = args[1] as CodeBlock
    assertThat(complexConstructorCode.formatParts()).containsExactly(
      "%T", "(",
      "courseId = ", "%S", ", ",
      "studentId = ", "%S",
      ")"
    )


    assertThat(complexConstructorCode.args()).containsExactly(
      courseAndStudentIdPoetType.typeName,
      "4711",
      "0815"
    )
  }


  @Test
  fun `fail to instantiate by missing value`() {
    val createCourseType = ctx.avroTypes[Name("CreateCourse")]!! as RecordType
    val createCoursePoetType = ctx.avroPoetTypes[createCourseType.hashCode]
    val thrown = assertThrows<IllegalArgumentException> {
      initializeMessage(
        createCoursePoetType, ctx.avroPoetTypes, jsonOf(
          """{
        "courseId": "4711",
        "name": "Course 1"
        }
        """.trimIndent()
        )
      )
    }
    assertThat(thrown.message).isEqualTo(
      "Failed to instantiate '${createCourseType.namespace.value + "." + createCourseType.name.value}'. "
        + "No value was supplied for field 'capacity' of type 'int'"
    )
  }

  @Test
  fun `fail to instantiate by null value`() {
    val createCourseType = ctx.avroTypes[Name("CreateCourse")]!! as RecordType
    val createCoursePoetType = ctx.avroPoetTypes[createCourseType.hashCode]
    val thrown = assertThrows<IllegalArgumentException> {
      initializeMessage(
        createCoursePoetType, ctx.avroPoetTypes, jsonOf(
          """{
        "courseId": null,
        "name": "Course 1",
        "capacity": 30
        }
        """.trimIndent()
        )
      )
    }
    assertThat(thrown.message).isEqualTo(
      "Failed to instantiate '${createCourseType.namespace.value + "." + createCourseType.name.value}'. "
        + "Field 'courseId' is not nullable, but value is null"
    )
  }

  @Test
  fun `fail to instantiate by wrong value type`() {
    val enrollType = ctx.avroTypes[Name("EnrollStudentToCourseAlternative")]!! as RecordType
    val enrollPoetType = ctx.avroPoetTypes[enrollType.hashCode]
    val thrown = assertThrows<IllegalArgumentException> {
      initializeMessage(
        enrollPoetType, ctx.avroPoetTypes, jsonOf(
          """{
        "courseAndStudentId": "4711",
        "name": "Course 1",
        "capacity": 30
        }
        """.trimIndent()
        )
      )
    }
    assertThat(thrown.message).isEqualTo(
      "Failed to instantiate '${enrollType.namespace.value + "." + enrollType.name.value}'. "
        + "Field 'courseAndStudentId' is of type 'CourseAndStudentCompositeKey', which can't be initialized from value '4711'"
    )
  }


  fun jsonOf(json: String): Map<String, Any?> {
    val om = ObjectMapper().registerKotlinModule()
    val map: Map<String, Any?> = om.readValue(
      json,
      om.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java)
    )
    return map
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

