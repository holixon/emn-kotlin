package io.holixon.emn.example.university.faculty.write.createcourse

import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.modelling.configuration.StatefulCommandHandlingModule

fun EventSourcingConfigurer.configureCreateCourse(): EventSourcingConfigurer {
  val stateEntity =
    EventSourcedEntityModule
      .annotated(
        CourseId::class.java,
        CreateCourseCommandHandler.State::class.java
      )

  val commandHandlingModule = StatefulCommandHandlingModule
    .named("CreateCourse")
    .entities()
    .entity(stateEntity)
    .commandHandlers()
    .annotatedCommandHandlingComponent { CreateCourseCommandHandler() }

  return this.registerStatefulCommandHandlingModule(commandHandlingModule)
}
