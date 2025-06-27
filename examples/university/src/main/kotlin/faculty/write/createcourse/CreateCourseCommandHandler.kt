package io.holixon.emn.example.university.faculty.write.createcourse

import io.holixon.emn.example.university.faculty.FacultyTags
import io.holixon.emn.example.university.faculty.events.CourseCreated
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.modelling.annotation.InjectEntity

class CreateCourseCommandHandler {

  @CommandHandler
  fun handle(command: CreateCourse, @InjectEntity(idProperty = FacultyTags.COURSE_ID) state: State, eventAppender: EventAppender) {
    val events: List<CourseCreated> = decide(command, state)
    eventAppender.append(events)
  }

  private fun decide(command: CreateCourse, state: State): List<CourseCreated> {
    if (state.created) {
      return listOf()
    }
    return listOf(CourseCreated(command.courseId, command.name, command.capacity))
  }

  @EventSourcedEntity(tagKey = FacultyTags.COURSE_ID)
  class State @EntityCreator constructor() {
    internal var created: Boolean = false

    @EventSourcingHandler
    private fun apply(event: CourseCreated): State {
      this.created = true
      return this
    }
  }
}
