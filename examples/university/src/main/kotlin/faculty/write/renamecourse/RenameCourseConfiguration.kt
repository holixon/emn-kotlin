package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.faculty.type.course.CourseId
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
  //.commandHandlingComponent(
  //    handlingComponentBuilder
  //  )
  return this.registerEntity(stateEntity)
    .registerCommandHandlingModule(commandHandlingModule)
}
