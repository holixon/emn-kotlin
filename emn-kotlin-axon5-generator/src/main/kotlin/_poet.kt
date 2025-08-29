@file:OptIn(ExperimentalKotlinPoetApi::class)

package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.MemberName
import io.toolisticon.kotlin.avro.generator.api.AvroPoetType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.buildAnnotation
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpec
import io.toolisticon.kotlin.generation.spec.KotlinAnnotationSpecSupplier
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventTag
import org.axonframework.modelling.annotation.InjectEntity
import org.axonframework.modelling.annotation.TargetEntityId

data class InjectEntityAnnotation(val idProperty: String? = null) : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(InjectEntity::class) {
    // we need an id property for creation command handler
    idProperty?.let { addStringMember("idProperty", it) }
  }
}

fun AvroPoetType.idProperty(): String? {
  // FIXME -> find a way how to model this.
  return null
}


data class EventTagAnnotation(val key: MemberName) : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventTag::class) {
    addMember("key = %M", key)
  }
}

object TargetEntityIdAnnotation : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(TargetEntityId::class)
}

/**
 * @EventSourcedEntity(
 *     tagKey = FacultyTags.COURSE_ID
 * )
 */
@OptIn(ExperimentalKotlinPoetApi::class)
data class EventSourcedEntityAnnotation(val key: MemberName) : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventSourcedEntity::class) {
    addMember("tagKey = %M", key)
  }
}

object EventSourcingHandlerAnnotation : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(EventSourcingHandler::class)
}

object CommandHandlerAnnotation : KotlinAnnotationSpecSupplier {
  override fun spec(): KotlinAnnotationSpec = buildAnnotation(CommandHandler::class)
}
