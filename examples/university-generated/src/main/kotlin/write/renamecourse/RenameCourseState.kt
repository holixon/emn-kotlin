package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseDoesNotExist
import io.holixon.emn.example.faculty.CourseRenamed
import io.holixon.emn.example.faculty.FacultyTags.COURSE
import io.holixon.emn.example.faculty.RenameCourse
import org.axonframework.eventsourcing.annotations.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventSourcingHandler
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

@EventSourcedEntity(tagKey = COURSE)
class RenameCourseState @EntityCreator constructor() {
  private var exists: Boolean = false
  private var currentName: String? = null

  fun decide(command: RenameCourse): List<Any> {
    if (!exists) {
      throw CourseDoesNotExist("Course with id=${command.courseId.value} does not exist.")
    }
    val newName = command.name
    return if (currentName == newName) {
      emptyList()
    } else {
      listOf(CourseRenamed(command.courseId, newName))
    }
  }

  @EventSourcingHandler
  fun evolve(event: CourseCreated): RenameCourseState = apply {
    exists = true
    currentName = event.name
  }

  @EventSourcingHandler
  fun evolve(event: CourseRenamed): RenameCourseState = apply {
    currentName = event.name
  }
}
