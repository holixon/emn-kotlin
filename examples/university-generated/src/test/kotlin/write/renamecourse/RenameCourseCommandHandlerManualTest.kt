package io.holixon.emn.example.faculty.write.renamecourse

import io.holixon.emn.example.faculty.*
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(AxonTestFixtureParameterResolver::class)
internal class RenameCourseCommandHandlerManualTest(val fixture: AxonTestFixture) {

  @Test
  fun givenNotExistingCourse_WhenRenameCourse_ThenException() {
    val courseId = CourseId.random()

    fixture.given()
      .noPriorActivity()
      .`when`()
      .command(RenameCourse(courseId, "Event Sourcing in Practice"))
      .then()
      .exception(RuntimeException::class.java, "Course with given id does not exist")
      .noEvents()
  }

  @Test
  fun givenCourseCreated_WhenRenameCourse_ThenSuccess() {
    val courseId = CourseId.random()

    fixture.given()
      .event(CourseCreated(courseId, "Event Sourcing in Practice", 42))
      .`when`()
      .command(RenameCourse(courseId, "Event Sourcing in Theory"))
      .then()
      .success()
      .events(CourseRenamed(courseId, "Event Sourcing in Theory"))
  }

  @Test
  fun givenCourseCreated_WhenRenameCourseToTheSameName_ThenSuccess_NoEvents() {
    val courseId = CourseId.random()

    fixture.given()
      .event(CourseCreated(courseId, "Event Sourcing in Practice", 42))
      .`when`()
      .command(RenameCourse(courseId, "Event Sourcing in Practice"))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun givenCourseCreatedAndRenamed_WhenRenameCourse_ThenSuccess() {
    val courseId = CourseId.random()

    fixture.given()
      .event(CourseCreated(courseId, "Event Sourcing in Practice", 42))
      .event(CourseRenamed(courseId, "Event Sourcing in Theory"))
      .`when`()
      .command(RenameCourse(courseId, "Theoretical Practice of Event Sourcing"))
      .then()
      .success()
      .events(CourseRenamed(courseId, "Theoretical Practice of Event Sourcing"))
  }
}
