package io.holixon.emn.example.university

import io.holixon.emn.example.university.faculty.type.course.CourseId
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.RenameCoursePolymorphCommandHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class AxonFrameworkTest {

  @Test
  fun `eventBuilderCriteria must be static - the right way`() {
    val method = RenameCoursePolymorphCommandHandler.State::class.java.getMethod("resolveCriteria", CourseId::class.java)
    assertThat(Modifier.isStatic(method.modifiers)).isTrue
  }

  @Test
  @Disabled("see https://github.com/AxonFramework/AxonFramework/issues/3680")
  fun `eventBuilderCriteria must be static - the wrong way`() {
    val method = RenameCoursePolymorphCommandHandler.State.javaClass.getMethod("resolveCriteria", CourseId::class.java)
    assertThat(Modifier.isStatic(method.modifiers)).isTrue
  }
}
