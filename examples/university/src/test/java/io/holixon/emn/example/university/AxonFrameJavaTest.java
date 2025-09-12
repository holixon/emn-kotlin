package io.holixon.emn.example.university;

import io.holixon.emn.example.university.faculty.type.course.CourseId;
import io.holixon.emn.example.university.faculty.write.renamecoursepolymorph.RenameCoursePolymorphCommandHandler;
import org.assertj.core.api.SoftAssertions;
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("see https://github.com/AxonFramework/AxonFramework/issues/3680")
public class AxonFrameJavaTest {

  /**
   * copied from AnnotationBasedEventCriteriaResolver for testing purposes
   */
  @SuppressWarnings("unchecked")
  public static List<Method> classAndStaticNestedClassesEventCriteriaBuilderMethods(Class<?> entityType) {
    Stream<Method> mainClassMethods = Arrays.stream(entityType.getDeclaredMethods()).filter((m) -> m.isAnnotationPresent(EventCriteriaBuilder.class));
    Stream<Method> nestedClassMethods = Arrays.stream(entityType.getDeclaredClasses()).filter((nestedClass) -> Modifier.isStatic(nestedClass.getModifiers())).flatMap((nestedClass) -> Arrays.stream(nestedClass.getDeclaredMethods())).filter((m) -> m.isAnnotationPresent(EventCriteriaBuilder.class));
    return Stream.concat(mainClassMethods, nestedClassMethods).collect(Collectors.toList());
  }

  @Test
  void eventBuilderCriteriaMustBeStatic() throws NoSuchMethodException {
    var method = RenameCoursePolymorphCommandHandler.State.class.getMethod("resolveCriteria", CourseId.class);

    assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
  }

  @Test
  void classAndStaticNestedClassesEventCriteriaBuilderMethodsReturnsValidResult() {
    var methods = classAndStaticNestedClassesEventCriteriaBuilderMethods(RenameCoursePolymorphCommandHandler.State.class);

    // Companion function fails
    SoftAssertions.assertSoftly(soft -> {
      methods.forEach(method -> {
        assertThat(Modifier.isStatic(method.getModifiers()))
          .as("not static: %s", method)
          .isTrue();
      });
    });
  }
}
