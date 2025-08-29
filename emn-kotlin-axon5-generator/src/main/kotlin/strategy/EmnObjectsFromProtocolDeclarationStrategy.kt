package io.holixon.emn.generation.strategy

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import io.holixon.emn.generation.ext.StringTransformations.TO_LOWER_CAMEL_CASE
import io.holixon.emn.generation.ext.StringTransformations.TO_UPPER_SNAKE_CASE
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.declaration.ProtocolDeclaration
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.generator.strategy.AvroFileSpecListFromProtocolDeclarationStrategy
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildObject
import io.toolisticon.kotlin.generation.poet.FormatSpecifier
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList


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
        emnContext.definitions.aggregates().mapNotNull { it.name }
          .distinct().forEach { name ->
            this.addProperty(TO_UPPER_SNAKE_CASE(name), String::class) {
              addModifiers(KModifier.CONST)
              initializer(FormatSpecifier.STRING, TO_LOWER_CAMEL_CASE(name))
            }
          }
      })
    }
    return KotlinFileSpecList(tagFile)
  }
}
