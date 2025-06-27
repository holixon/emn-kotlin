package io.holixon.emn.example.university.faculty.write.createcourse

import io.holixon.emn.example.university.faculty.type.course.CourseId

data class CreateCourse(
  val courseId: CourseId,
  val name: String,
  val capacity: Int
)
