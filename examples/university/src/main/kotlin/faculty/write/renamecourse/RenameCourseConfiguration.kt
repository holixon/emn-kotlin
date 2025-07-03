package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.commandhandling.CommandHandlingComponent
import org.axonframework.configuration.ComponentFactory
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.modelling.configuration.StatefulCommandHandlingModule


fun EventSourcingConfigurer.configureRenameCourse(): EventSourcingConfigurer {
  val stateEntity =
    EventSourcedEntityModule
      .annotated(
        CourseId::class.java,
        RenameCourseCommandHandler.State::class.java
      )


  val commandHandlingModule = StatefulCommandHandlingModule
    .named("RenameCourse")
    .entities()
    .entity(stateEntity)
    .commandHandlers()
    .annotatedCommandHandlingComponent { RenameCourseCommandHandler() }
  //.commandHandlingComponent(
  //    handlingComponentBuilder
  //  )
  return this.registerStatefulCommandHandlingModule(commandHandlingModule)
}
