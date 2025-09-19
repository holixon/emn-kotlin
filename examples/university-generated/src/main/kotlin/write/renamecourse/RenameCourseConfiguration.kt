package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseId
import io.holixon.emn.example.faculty.write.createcourse.CreateCourseCommandHandler
import org.axonframework.commandhandling.configuration.CommandHandlingModule
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer

fun EventSourcingConfigurer.configureRenameCourse(): EventSourcingConfigurer {
  val stateEntity =
    EventSourcedEntityModule
      .annotated(
        CourseId::class.java,
        RenameCourseCommandHandler.State::class.java
      )

  val commandHandlingModule = CommandHandlingModule
    .named("RenameCourse")
    .commandHandlers()
    .annotatedCommandHandlingComponent { RenameCourseCommandHandler() }

  return this.registerCommandHandlingModule(commandHandlingModule)
    .registerEntity(stateEntity)
}
