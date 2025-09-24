package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import io.holixon.emn.generation.avro.ProtocolDeclarationContextExt.emnContext
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.declaration.ProtocolDeclaration
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.generator.strategy.AvroFileSpecListFromProtocolDeclarationStrategy
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildFile
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildObject
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.constantName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.propertyName
import io.toolisticon.kotlin.generation.poet.FormatSpecifier
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnObjectsFromProtocolDeclarationStrategy : AvroFileSpecListFromProtocolDeclarationStrategy() {

  override fun invoke(
    context: ProtocolDeclarationContext,
    input: ProtocolDeclaration
  ): KotlinFileSpecList {
    val emnContext = context.emnContext

    val tagClassName = emnContext.getTagClassName()

    val tagFile = buildFile(tagClassName) {
      addType(buildObject(tagClassName) {
        emnContext.definitions.aggregates().mapNotNull { it.name }
          .distinct().forEach { name ->
            this.addProperty(constantName(name), String::class) {
              addModifiers(KModifier.CONST)
              initializer(FormatSpecifier.STRING, propertyName(name))
            }
          }
      })
    }

    return KotlinFileSpecList(tagFile)
  }
}
