package io.holixon.emn.example.faculty.write.createcourse

import io.holixon.emn.example.faculty.AxonTestFixtureParameterResolver
import io.holixon.emn.example.faculty.CourseCreated
import io.holixon.emn.example.faculty.CourseId
import io.holixon.emn.example.faculty.CreateCourse
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(AxonTestFixtureParameterResolver::class)
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
