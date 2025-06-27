package io.holixon.emn.example.university.faculty.events

import io.holixon.emn.example.university.faculty.FacultyTags
import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.eventsourcing.annotations.EventTag

class CourseRenamed(
  @EventTag(key = FacultyTags.COURSE_ID)
  val courseId: CourseId,
  val name: String
)
