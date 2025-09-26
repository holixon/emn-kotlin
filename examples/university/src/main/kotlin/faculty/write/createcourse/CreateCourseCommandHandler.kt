package io.holixon.emn.example.university.faculty.write.createcourse

import io.holixon.emn.example.university.faculty.FacultyTags
import io.holixon.emn.example.university.faculty.events.CourseCreated
import io.holixon.emn.example.university.infrastructure.DecidingState
import org.axonframework.commandhandling.annotations.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.annotations.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventSourcingHandler
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator
import org.axonframework.modelling.annotations.InjectEntity

class CreateCourseCommandHandler {

  @CommandHandler
  fun handle(command: CreateCourse, @InjectEntity(idProperty = FacultyTags.COURSE_ID) state: State, eventAppender: EventAppender) {
    eventAppender.append(state.decide(command))
  }

  @EventSourcedEntity(
    tagKey = FacultyTags.COURSE_ID
  )
  class State @EntityCreator constructor() : DecidingState <CreateCourse> {

    private var created: Boolean = false

    override fun decide(command: CreateCourse): List<Any> {
      if (created) {
        return listOf()
      }
      return listOf(CourseCreated(command.courseId, command.name, command.capacity))
    }

    @EventSourcingHandler
    fun apply(event: CourseCreated): State {
      this.created = true
      return this
    }

  }
}
