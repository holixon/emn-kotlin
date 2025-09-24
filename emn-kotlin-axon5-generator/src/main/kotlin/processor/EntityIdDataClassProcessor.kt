package io.holixon.emn.generation.processor

import com.squareup.kotlinpoet.*
import io.holixon.emn.generation.avro.SchemaDeclarationContextExt.entityName
import io.holixon.emn.generation.avro.SchemaDeclarationContextExt.isEntityId
import io.toolisticon.kotlin.avro.generator.processor.KotlinDataClassFromRecordTypeProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildCompanionObject
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.name.constantName
import io.toolisticon.kotlin.generation.builder.KotlinDataClassSpecBuilder
import io.toolisticon.kotlin.generation.poet.FormatSpecifier
import java.util.*

@OptIn(ExperimentalKotlinPoetApi::class)
class EntityIdDataClassProcessor : KotlinDataClassFromRecordTypeProcessorBase() {
  override fun invoke(context: SchemaDeclarationContext, input: RecordType, builder: KotlinDataClassSpecBuilder): KotlinDataClassSpecBuilder {
    input.documentation?.let { builder.addKdoc(it.value) }
    val poetType = context.avroPoetTypes[input.hashCode]
    val className = poetType.typeName as ClassName

    val entityName = context.entityName(input)

    builder.addType(buildCompanionObject {
      addProperty("ENTITY_ID", String::class) {
        addModifiers(KModifier.CONST)
        initializer(FormatSpecifier.STRING, constantName(entityName))
      }
      addFunction("random") {
        returns(poetType.typeName)
        val uuid = UUID::class.asClassName()
        val method = MemberName(uuid, "randomUUID")
        addStatement(
          $$"return %T(\"${%N}:${%M()}\")",
          className,
          MemberName(className, "ENTITY_ID"),
          method
        )
      }
    })

    return builder
  }

  override fun test(context: SchemaDeclarationContext, input: Any): Boolean {
    return super.test(context, input) && context.isEntityId(input)
  }
}
