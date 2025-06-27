package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.faculty.FacultyTags
import io.holixon.emn.example.university.faculty.events.CourseCreated
import io.holixon.emn.example.university.faculty.events.CourseRenamed
import io.holixon.emn.example.university.infrastructure.SealedClassEventSourcedFactoryDefinition
import org.axonframework.commandhandling.annotation.CommandHandler
import org.axonframework.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.EventSourcedEntity
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.modelling.annotation.InjectEntity

class RenameCourseCommandHandler {

  @CommandHandler
  fun handle(command: RenameCourse, @InjectEntity state: State, eventAppender: EventAppender) {
    val events = state.decide(command)
    eventAppender.append(events)
  }

  @EventSourcedEntity(
    tagKey = FacultyTags.COURSE_ID
  )
  class State @EntityCreator constructor() {

    var created: Boolean = false
    var name: String = ""

    @EventSourcingHandler
    fun apply(event: CourseCreated): State {
      this.created = true
      this.name = event.name
      return this
    }

    @EventSourcingHandler
    fun apply(event: CourseRenamed): State {
      this.name = event.name
      return this
    }

    fun decide(command: RenameCourse): List<CourseRenamed> {
      return if (!this.created) {
        throw IllegalStateException("Course with given id does not exist")
      } else {
        if (command.name == name) {
          listOf()
        } else {
          listOf(CourseRenamed(command.courseId, command.name))
        }
      }
    }

  }

}

