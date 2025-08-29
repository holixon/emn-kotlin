package io.holixon.emn.generation.processor

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import io.github.oshai.kotlinlogging.KotlinLogging
import io.holixon.emn.generation.EventTagAnnotation
import io.holixon.emn.generation.TargetEntityIdAnnotation
import io.holixon.emn.generation.conflictingAggregatesFound
import io.holixon.emn.generation.getAvroTypeDefinitionRef
import io.holixon.emn.generation.noAggregateFoundLogger
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.CommandType
import io.holixon.emn.model.EventType
import io.holixon.emn.model.applyIfExactlyOne
import io.toolisticon.kotlin.avro.generator.processor.ConstructorPropertyFromRecordFieldProcessorBase
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordField
import io.toolisticon.kotlin.avro.value.CanonicalName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.builder.KotlinConstructorPropertySpecBuilder
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpec
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpecSupplier
import io.toolisticon.kotlin.generation.tag
import org.axonframework.eventsourcing.annotations.EventTag
import org.axonframework.modelling.annotation.TargetEntityId

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalKotlinPoetApi::class)
class ConstructorPropertyAnnotationProcessor : ConstructorPropertyFromRecordFieldProcessorBase() {
  override fun invoke(
    context: SchemaDeclarationContext,
    input: RecordField,
    builder: KotlinConstructorPropertySpecBuilder
  ): KotlinConstructorPropertySpecBuilder = builder.apply {

    val emnContext: EmnGenerationContext = context.tag()!!
    val recordType = input.memberOf
    val emnElementType = emnContext.getEmnType(recordType)



    when (emnElementType) {
      is EventType -> {
        val aggregateLanes = emnContext.definitions.aggregates(emnElementType).distinct()
        aggregateLanes.applyIfExactlyOne(
          logger.noAggregateFoundLogger(emnElementType),
          logger.conflictingAggregatesFound(emnElementType)
        ) { aggregateLane ->
          var aggregateIdCanonicalName = aggregateLane.idSchema.getAvroTypeDefinitionRef()?.content?.let {
            CanonicalName.parse(it)
          }
          if (input.schema.canonicalName == aggregateIdCanonicalName && aggregateLane.name != null) {
            val tagMember = emnContext.resolveAggregateTagName(aggregateLane)
            addAnnotation(EventTagAnnotation(key = tagMember))
          }
        }
      }

      is CommandType -> {
        val aggregateLanes = emnContext.definitions.aggregates(emnElementType).distinct()
        aggregateLanes.applyIfExactlyOne(
          logger.noAggregateFoundLogger(emnElementType),
          logger.conflictingAggregatesFound(emnElementType)
        ) { aggregateLane ->
          var aggregateIdCanonicalName = aggregateLane.idSchema.getAvroTypeDefinitionRef()?.content?.let {
            CanonicalName.parse(it)
          }
          val aggregateName = aggregateLane.name
          if (input.schema.canonicalName == aggregateIdCanonicalName && aggregateName != null) {
            addAnnotation(TargetEntityIdAnnotation)
          }
        }
      }

      else -> {
        // skipping, not event and not command
      }
    }

    addKdoc("Constructor property for field '${input.name.value}' of record type '${recordType.name.value}'.")
  }

  override fun test(context: SchemaDeclarationContext, input: Any): Boolean {
    return super.test(context, input)
  }

}
