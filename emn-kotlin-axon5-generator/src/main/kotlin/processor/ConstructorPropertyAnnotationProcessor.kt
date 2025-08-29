package io.holixon.emn.generation.processor

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import io.holixon.emn.generation.embeddedAvroSchema
import io.holixon.emn.generation.ext.StringTransformations
import io.holixon.emn.generation.ext.StringTransformations.TO_LOWER_CAMEL_CASE
import io.holixon.emn.generation.ext.StringTransformations.TO_LOWER_CAMEL_CASE.invoke
import io.holixon.emn.generation.ext.StringTransformations.TO_UPPER_SNAKE_CASE
import io.holixon.emn.generation.ext.StringTransformations.TO_UPPER_SNAKE_CASE.invoke
import io.holixon.emn.generation.hasAvroTypeDefinition
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.EventType
import io.toolisticon.kotlin.avro.generator.processor.ConstructorPropertyFromRecordFieldProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordField
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildObject
import io.toolisticon.kotlin.generation.builder.KotlinConstructorPropertySpecBuilder
import io.toolisticon.kotlin.generation.poet.FormatSpecifier
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpec
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpecSupplier
import io.toolisticon.kotlin.generation.tag
import org.axonframework.eventsourcing.annotations.EventTag

@OptIn(ExperimentalKotlinPoetApi::class)
class ConstructorPropertyAnnotationProcessor : ConstructorPropertyFromRecordFieldProcessorBase() {
  override fun invoke(
    context: SchemaDeclarationContext,
    input: RecordField,
    builder: KotlinConstructorPropertySpecBuilder
  ): KotlinConstructorPropertySpecBuilder = builder.apply {

    val emnCtx: EmnGenerationContext = context.tag()!!
    val recordType = input.memberOf
    val eventType = emnCtx.avroReferenceTypes[recordType.canonicalName]

    try {
      if (eventType is EventType) {
        val aggregateLane = emnCtx.events.filter { it.hasAvroTypeDefinition() }
          .filter { it.typeReference == eventType }
          .map { emnCtx.definitions.aggregates(it) }
          .flatten()
          .single()




        val aggregateIdFullQualifiedName = aggregateLane?.idSchema?.embeddedAvroSchema()?.content
        if (aggregateIdFullQualifiedName != null && input.schema.canonicalName == CanonicalName.parse(aggregateIdFullQualifiedName)) {
          val tagClassName = KotlinCodeGeneration.className(
            packageName = emnCtx.properties.rootPackageName,
            simpleName = PropertyNamingStrategies.UpperCamelCaseStrategy().translate(emnCtx.properties.emnName + "Tags")
          )
          val tagMember = MemberName(tagClassName, TO_UPPER_SNAKE_CASE(aggregateLane.name!!))


          addAnnotation(EventTagAnnotation(key = tagMember))
        }
      }
    } catch(e:Exception) {
      println("Could not determine EventTag for ${recordType.canonicalName}: ${e.message}")

    }


    addKdoc("Constructor property for field '${input.name.value}' of record type '${recordType.name.value}'.")
//    addKdoc("Ctx: ${emnCtx.definitions} ")
  }

  override fun test(context: SchemaDeclarationContext, input: Any): Boolean {
    return super.test(context, input)
  }

  @OptIn(ExperimentalKotlinPoetApi::class)
  data class EventTagAnnotation(val key: MemberName) : KotlinAnnotationSpecSupplier {
    override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventTag::class) {
      addMember("key=%M", key)
    }
  }
}
