package io.holixon.emn.generation

import com.facebook.ktfmt.format.Formatter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.TestFixtures.getField
import io.toolisticon.kotlin.avro.AvroParser
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import java.lang.reflect.Field
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

@OptIn(ExperimentalKotlinPoetApi::class)
data object TestFixtures {

  val logger = KotlinLogging.logger {}

  // loads ALL available strategies and processors for ALL contexts
  val SPI_REGISTRY = load()

  object AvroKotlinFixtures {

    val AVRO_PARSER = AvroParser()

  }

  val EMN_PARSER = EmnDocumentParser()

  val KTFMT_FORMAT = Formatter.KOTLINLANG_FORMAT.copy(
    maxWidth = 256,
    blockIndent = 2,
    continuationIndent = 2
  )

  fun KotlinFileSpec.writeTo(generatedSourcesDir: Path, overwrite: Boolean = false) {
    val file = this.get().writeTo(generatedSourcesDir)
    //println("Written file to $file")

//    file.writeText(Formatter.format(KTFMT_FORMAT, this.code))
    file.writeText(this.code)
  }

  fun createGeneratedSourcesDir(): Path {
    val moduleRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
    val dir = moduleRoot.resolve("target").resolve("generated-test-sources").resolve("emn")
    createDirectories(dir)
    return dir
  }

  fun jsonOf(json: String): Map<String, Any?> {
    val om = ObjectMapper().registerKotlinModule()
    val map: Map<String, Any?> = om.readValue(
      json,
      om.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java)
    )
    return map
  }


  @Suppress("UNCHECKED_CAST")
  fun <T> getField(instance: Any, fieldName: String): T? {
    val field: Field = instance.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(instance) as? T
  }

}

fun CodeBlock.formatParts() = getField<List<String>>(this, "formatParts")
fun CodeBlock.args() = getField<List<Any>>(this, "args")
