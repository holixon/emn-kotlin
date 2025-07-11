package io.holixon.emn.generation.strategy

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asClassName
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.declaration.ProtocolDeclaration
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.generator.strategy.AvroFileSpecListFromProtocolDeclarationStrategy
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildObject
import io.toolisticon.kotlin.generation.poet.FormatSpecifier
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.support.CodeBlockArray


@OptIn(ExperimentalKotlinPoetApi::class)
class EmnObjectsFromProtocolDeclarationStrategy : AvroFileSpecListFromProtocolDeclarationStrategy() {


  override fun invoke(
    context: ProtocolDeclarationContext,
    input: ProtocolDeclaration
  ): KotlinFileSpecList {
    val emnContext = context.tag(EmnGenerationContext::class)!!

    val tagClassName = KotlinCodeGeneration.className(
      packageName = emnContext.properties.rootPackageName,
      simpleName = PropertyNamingStrategies.UpperCamelCaseStrategy().translate(emnContext.properties.emnName + "Tags")
    )

    val tagFile = KotlinCodeGeneration.buildFile(tagClassName) {
      addType(buildObject(tagClassName) {
        input.avroTypes.findTypes<RecordType> { it.name.value.endsWith("Id") }
          .map { it.name.value }.distinct().forEach { name ->
            this.addProperty(PropertyNamingStrategies.UpperSnakeCaseStrategy().translate(name), String::class) {
              addModifiers(KModifier.CONST)
              initializer(FormatSpecifier.STRING, PropertyNamingStrategies.LowerCamelCaseStrategy().translate(name).replaceFirstChar { it.lowercaseChar() })
            }
          }
      })
    }
    return KotlinFileSpecList(tagFile)
  }
}
