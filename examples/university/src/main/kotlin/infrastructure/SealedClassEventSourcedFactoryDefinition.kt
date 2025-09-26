package io.holixon.emn.example.university.infrastructure

import jakarta.annotation.Nonnull
import org.axonframework.configuration.Configuration
import org.axonframework.eventhandling.conversion.EventConverter
import org.axonframework.eventsourcing.EventSourcedEntityFactory
import org.axonframework.eventsourcing.annotations.EventSourcedEntityFactoryDefinition
import org.axonframework.eventsourcing.annotations.reflection.AnnotationBasedEventSourcedEntityFactory
import org.axonframework.messaging.MessageTypeResolver
import org.axonframework.messaging.annotations.ParameterResolverFactory

class SealedClassEventSourcedFactoryDefinition<ENTITY, ID> : EventSourcedEntityFactoryDefinition<ENTITY, ID> {

  override fun createFactory(
    @Nonnull entityType: Class<ENTITY>,
    @Nonnull entitySubTypes: Set<Class<out ENTITY>>,
    @Nonnull idType: Class<ID>,
    @Nonnull configuration: Configuration
  ): EventSourcedEntityFactory<ID, ENTITY> {

    val subTypes: Set<Class<out ENTITY>> = if (entityType.isSealed) {
      @Suppress("UNCHECKED_CAST")
      entitySubTypes + entityType.permittedSubclasses.toSet() as Set<Class<out ENTITY>>
    } else {
      entitySubTypes
    }

    return AnnotationBasedEventSourcedEntityFactory(
      entityType,
      idType,
      subTypes,
      configuration.getComponent(ParameterResolverFactory::class.java),
      configuration.getComponent(MessageTypeResolver::class.java),
      configuration.getComponent(EventConverter::class.java),
    )
  }


}
