package io.holixon.emn.example.university

import io.holixon.emn.example.university.command.CreateCourse
import io.holixon.emn.example.university.common.CourseType
import io.holixon.emn.example.university.event.CourseCreated
import io.holixon.emn.example.university.query.CourseOverview
import io.holixon.emn.example.university.query.CourseOverviewItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class GeneratedSchemaTypesTest {

  @Test
  fun `generated schema classes can be instantiated`() {
    val courseId = UUID.fromString("9b6ecaa8-bb6d-4d65-92e7-0d10ba9cb7ba")
    val createdAt = OffsetDateTime.parse("2026-09-04T09:00:00Z")
    val command = CreateCourse(
      courseId = courseId,
      name = "Java 101",
      courseType = CourseType.LIVE,
      capacity = 5,
      createdAt = createdAt
    )
    val event = CourseCreated(
      courseId = command.courseId,
      name = command.name,
      courseType = command.courseType,
      capacity = command.capacity,
      createdAt = command.createdAt
    )
    val query = listOf(
      CourseOverviewItem(
        courseId = event.courseId,
        name = event.name,
        courseType = event.courseType,
        capacity = event.capacity,
        createdAt = event.createdAt,
        students = 0
      )
    )
    val courseOverview: CourseOverview = query
    val queryItem = courseOverview.single()

    assertThat(queryItem.courseId).isEqualTo(courseId)
    assertThat(queryItem.courseType).isEqualTo(CourseType.LIVE)
    assertThat(queryItem.createdAt).isEqualTo(createdAt)
    assertThat(queryItem.students).isZero()
  }
}
