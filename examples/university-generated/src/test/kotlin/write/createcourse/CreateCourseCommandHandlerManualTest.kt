package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseId
import io.holixon.emn.example.faculty.CreateCourse
import io.holixon.emn.example.faculty.DuplicateCourse
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Not needed, just for demonstration of Axon Test Fixture usage")
internal class CreateCourseCommandHandlerManualTest(val fixture: AxonTestFixture) {

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
  fun givenNotExistingCourse_WhenCreateCourse_ThenSuccess1() {
    with(fixture) {
      val courseId = CourseId.random()
      val courseName = "Event Sourcing in Practice"
      val capacity = 3

      given()
        .noPriorActivity()
        .`when`()
        .command(CreateCourse(courseId, courseName, capacity))
        .then()
        .success()
        .events(CourseCreated(courseId, courseName, capacity))
    }
  }

  @Test
  fun givenCourseCreated_WhenCreateCourse_ThenErrorDuplicateCourse() {
    val courseId = CourseId.random()
    val courseName = "Event Sourcing in Practice"
    val capacity = 3

    fixture.given()
      .event(CourseCreated(courseId, courseName, capacity))
      .`when`()
      .command(CreateCourse(courseId, courseName, capacity))
      .then()
      .noEvents()
      .exception(DuplicateCourse::class.java)
  }
}
