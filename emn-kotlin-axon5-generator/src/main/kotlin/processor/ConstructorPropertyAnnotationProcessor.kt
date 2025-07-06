package io.holixon.emn.generation.processor

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.generator.processor.ConstructorPropertyFromRecordFieldProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordField
import io.toolisticon.kotlin.generation.builder.KotlinConstructorPropertySpecBuilder
import io.toolisticon.kotlin.generation.tag

@OptIn(ExperimentalKotlinPoetApi::class)
class ConstructorPropertyAnnotationProcessor : ConstructorPropertyFromRecordFieldProcessorBase() {
  override fun invoke(
    context: SchemaDeclarationContext,
    input: RecordField,
    builder: KotlinConstructorPropertySpecBuilder
  ): KotlinConstructorPropertySpecBuilder = builder.apply {
    val emnCtx : EmnGenerationContext = context.tag()!!
    val recordType = input.memberOf

    addKdoc("Constructor property for field '${input.name.value}' of record type '${recordType.name.value}'.")
    addKdoc("Ctx: ${emnCtx.definitions} ")

  }


}
