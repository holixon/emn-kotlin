package io.holixon.emn.example.university.faculty.write.renamecourse

import io.holixon.emn.example.university.UniversityAxonApplication
import io.holixon.emn.example.university.faculty.events.CourseCreated
import io.holixon.emn.example.university.faculty.events.CourseRenamed
import io.holixon.emn.example.university.faculty.type.course.CourseId
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RenameCourseCommandHandlerTest {

  private lateinit var fixture: AxonTestFixture

  @BeforeEach
  fun beforeEach() {
    fixture = AxonTestFixture.with(UniversityAxonApplication().configurer())
  }


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
