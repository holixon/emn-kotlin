package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseDoesNotExist
import io.holixon.emn.example.faculty.CourseId
import io.holixon.emn.example.faculty.CourseRenamed
import io.holixon.emn.example.faculty.FacultyTags.COURSE
import io.holixon.emn.example.faculty.RenameCourse
import org.axonframework.eventsourcing.annotations.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventSourcingHandler
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator
import org.axonframework.spring.stereotype.EventSourced
import org.springframework.stereotype.Component

@EventSourced(tagKey = COURSE, idType = CourseId::class)
class RenameCourseState @EntityCreator constructor() {
  private var created: Boolean = false
  private var name: String = ""

  fun decide(command: RenameCourse): List<Any> {
    return if (!this.created) {
      throw CourseDoesNotExist("Course with id=${command.courseId.value} does not exist.")
    } else {
      if (command.name == name) {
        listOf()
      } else {
        listOf(
          CourseRenamed(command.courseId, command.name)
        )
      }
    }
  }

  @EventSourcingHandler
  fun evolve(event: CourseRenamed) = apply{
    this.created = true
    this.name = event.name
    return this
  }

  @EventSourcingHandler
  fun evolve(event: CourseCreated)= apply {
    this.created = true
    this.name = event.name
    return this
  }

}
