package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.TypeName
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import io.holixon.emn.generation.TestFixtures.jsonOf
import io.holixon.emn.generation.TestFixtures.logger
import io.toolisticon.kotlin.avro.generator.DefaultAvroKotlinGeneratorProperties
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.avro.value.Name
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.backend.konan.InteropFqNames.TypeName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Field

@OptIn(ExperimentalKotlinPoetApi::class)
class PoetInstancioMessageInstantiationTest {

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

    val block = instantiateMessageWithInstancio(
      createCoursePoetType, ctx.avroPoetTypes, jsonOf(
        """{
        "courseId": "4711"
        }
        """.trimIndent()
      )
    )
    logger.info { "Block: $block" }

    assertThat(block.formatParts()).containsExactly(
      "%T",
      ".create(", "%T", "::class.java)",
      ".copy(",
      "courseId = ", "%L",
      ")"
    )
    val args = block.args()
    assertThat(args).isNotNull.isNotEmpty.hasSize(3)
    assertThat(args).contains(
      ClassName("org.instancio", "Instancio"),
      createCoursePoetType.typeName,
    )
    assertThat(args!![2]).isInstanceOf(CodeBlock::class.java)
    val complexConstructorCode: CodeBlock = args[2] as CodeBlock
    assertThat(complexConstructorCode.formatParts()).containsExactly("%T", "(", "%S", ")")


    assertThat(complexConstructorCode.args()).containsExactly(
      courseIdPoetType.typeName,
      "4711"
    )
  }

}

