package io.holixon.emn.example.university

import io.holixon.emn.example.university.command.CommandType_create_course
import io.holixon.emn.example.university.event.EventType_course_created
import io.holixon.emn.example.university.query.QueryType_courses
import io.holixon.emn.example.university.query.QueryType_coursesItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneratedSchemaTypesTest {

  @Test
  fun `generated schema classes can be instantiated`() {
    val command = CommandType_create_course(
      courseId = "course-4711",
      name = "Java 101",
      capacity = 5
    )
    val event = EventType_course_created(
      courseId = command.courseId,
      name = command.name,
      capacity = command.capacity
    )
    val query = listOf(
      QueryType_coursesItem(
        courseId = event.courseId,
        name = event.name,
        capacity = event.capacity
      )
    )
    val courseOverview: QueryType_courses = query
    val queryItem = courseOverview.single()

    assertThat(queryItem.courseId).isEqualTo("course-4711")
    assertThat(queryItem.capacity).isEqualTo(5)
  }
}
