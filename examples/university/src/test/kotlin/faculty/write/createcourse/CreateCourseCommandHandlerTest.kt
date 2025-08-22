package io.holixon.emn.example.university.faculty.write.createcourse

import io.holixon.emn.example.university.UniversityAxonApplication
import io.holixon.emn.example.university.faculty.events.CourseCreated
import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class CreateCourseCommandHandlerTest {
  private lateinit var fixture: AxonTestFixture

  @BeforeEach
  fun beforeEach() {
    fixture = AxonTestFixture.with(UniversityAxonApplication().configurer())
  }

  @Test
  fun givenNotExistingCourse_WhenCreateCourse_ThenSuccess() {
    val courseId = CourseId.random()
    val courseName = "Event Sourcing in Practice"
    val capacity = 3

    fixture.given()
      .`when`()
      .command(CreateCourse(courseId, courseName, capacity))
      .then()
      .success()
      .events(CourseCreated(courseId, courseName, capacity))
  }

  @Test
  fun givenCourseCreated_WhenCreateCourse_ThenSuccess_NoEvents() {
    val courseId = CourseId.random()
    val courseName = "Event Sourcing in Practice"
    val capacity = 3

    fixture.given()
      .event(CourseCreated(courseId, courseName, capacity))
      .`when`()
      .command(CreateCourse(courseId, courseName, capacity))
      .then()
      .success()
      .noEvents()
  }
}
