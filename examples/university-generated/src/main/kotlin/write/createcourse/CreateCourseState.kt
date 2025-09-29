package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CreateCourse
import io.holixon.emn.example.faculty.DuplicateCourse
import io.holixon.emn.example.faculty.FacultyTags.COURSE
import org.axonframework.eventsourcing.annotations.EventSourcedEntity
import org.axonframework.eventsourcing.annotations.EventSourcingHandler
import org.axonframework.eventsourcing.annotations.reflection.EntityCreator

@EventSourcedEntity(tagKey = COURSE)
class CreateCourseState @EntityCreator constructor() {
  private var created: Boolean = false

  fun decide(command: CreateCourse): List<Any> =
    if (created) {
      throw DuplicateCourse("Course with id=${command.courseId.value} already exists.")
    } else {
      listOf(CourseCreated(command.courseId, command.name, command.capacity))
    }

  @EventSourcingHandler
  fun evolve(event: CourseCreated): CreateCourseState = apply { created = true }
}
