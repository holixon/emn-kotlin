package io.holixon.emn.example.university.faculty.write.renamecoursepolymorph

import io.holixon.emn.example.university.faculty.type.course.CourseId
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.RenameCoursePolymorphCommandHandler.State
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.modelling.configuration.StatefulCommandHandlingModule


fun EventSourcingConfigurer.configureRenameCoursePolymorph(): EventSourcingConfigurer {
  val stateEntity =
    EventSourcedEntityModule
      /*
      .declarative(CourseId::class.java, State::class.java)
      .messagingModel(
        EntityMetamodelConfigurationBuilder {
          configuration, s -> s
        }
      ).entityFactory {
        SealedClassEventSourcedFactoryDefinition()
          .createFactory(State::class.java, setOf(), CourseId::class, null)
      }
*/
      .annotated(
        CourseId::class.java,
        State::class.java
      )


  val commandHandlingModule = StatefulCommandHandlingModule
    .named("RenameCoursePolymorph")
    .entities()
    .entity(stateEntity)
    .commandHandlers()
    .annotatedCommandHandlingComponent { RenameCoursePolymorphCommandHandler() }

  //.commandHandlingComponent(
  //    handlingComponentBuilder
  //  )
  return this.registerStatefulCommandHandlingModule(commandHandlingModule)
}
