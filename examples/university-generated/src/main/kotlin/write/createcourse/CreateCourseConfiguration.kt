package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.CourseId
import org.axonframework.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer

fun EventSourcingConfigurer.configureCreateCourse(): EventSourcingConfigurer {
  val stateEntity =
    EventSourcedEntityModule
      .annotated(
        CourseId::class.java,
        CreateCourseCommandHandler.State::class.java
      )

  val commandHandlingModule = CommandHandlingModule
    .named("CreateCourse")
    .commandHandlers()
    .annotatedCommandHandlingComponent { CreateCourseCommandHandler() }

  return this.registerCommandHandlingModule(commandHandlingModule)
    .registerEntity(stateEntity)
}
