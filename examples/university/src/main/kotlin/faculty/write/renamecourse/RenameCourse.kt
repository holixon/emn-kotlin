package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.modelling.annotations.TargetEntityId

data class RenameCourse(
  @TargetEntityId
  val courseId: CourseId,
  val name: String
)
