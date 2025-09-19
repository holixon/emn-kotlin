package io.holixon.emn.generation

import io.holixon.emn.model.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ExtensionsTest {

  @Test
  fun `test testMethodName with events in given stage`() {
    // Create a specification with events in the given stage
    val event1 = Event(
      id = "event1",
      typeReference = EventType(id = "event1", name = "Event1", schema = null),
      value = null
    )
    val event2 = Event(
      id = "event2",
      typeReference = EventType(id = "event2", name = "Event2", schema = null),
      value = null
    )
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )
    val resultEvent = Event(
      id = "event3",
      typeReference = EventType(id = "event3", name = "Event3", schema = null),
      value = null
    )
    val resultEvent2 = Event(
      id = "event4",
      typeReference = EventType(id = "event4", name = "Event4", schema = null),
      value = null
    )

    val specification = Specification(
      id = "spec1",
      name = "Test Specification",
      givenStage = GivenStage(
        id = "given1",
        values = listOf(
          event1,
          event2,
          // Add a non-event element to verify it's filtered out
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = WhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = ThenStage(
        id = "then1",
        values = listOf(resultEvent, resultEvent2)
      )
    )

    // Verify the test method name
    assertThat(specification.testMethodName).isEqualTo("givenEvent1AndEvent2_whenCommand1_thenEvent3AndEvent4")
  }

  @Test
  fun `test testMethodName with events in given stage but no events in then stage`() {
    // Create a specification with events in the given stage
    val event1 = Event(
      id = "event1",
      typeReference = EventType(id = "event1", name = "Event1", schema = null),
      value = null
    )
    val event2 = Event(
      id = "event2",
      typeReference = EventType(id = "event2", name = "Event2", schema = null),
      value = null
    )
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )

    val specification = Specification(
      id = "spec1",
      name = "Test Specification",
      givenStage = GivenStage(
        id = "given1",
        values = listOf(
          event1,
          event2,
          // Add a non-event element to verify it's filtered out
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = WhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = ThenStage(
        id = "then1",
        values = listOf()
      )
    )

    // Verify the test method name
    assertThat(specification.testMethodName).isEqualTo("givenEvent1AndEvent2_whenCommand1_thenNoEvents")
  }

  @Test
  fun `test testMethodName with events in given stage and error in then stage`() {
    // Create a specification with events in the given stage
    val event1 = Event(
      id = "event1",
      typeReference = EventType(id = "event1", name = "Event1", schema = null),
      value = null
    )
    val event2 = Event(
      id = "event2",
      typeReference = EventType(id = "event2", name = "Event2", schema = null),
      value = null
    )
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )

    val error = Error(
      id = "error",
      typeReference = ErrorType(id = "error1", name = "Error1", schema = null),
      value = null
    )

    val specification = Specification(
      id = "spec1",
      name = "Test Specification",
      givenStage = GivenStage(
        id = "given1",
        values = listOf(
          event1,
          event2,
          // Add a non-event element to verify it's filtered out
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = WhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = ThenStage(
        id = "then1",
        values = listOf(error)
      )
    )

    // Verify the test method name
    assertThat(specification.testMethodName).isEqualTo("givenEvent1AndEvent2_whenCommand1_thenError1")
  }

  @Test
  fun `test testMethodName with no events in given stage`() {
    // Create a specification with no events in the given stage
    val command = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )
    val resultEvent = Event(
      id = "event3",
      typeReference = EventType(id = "event3", name = "Event3", schema = null),
      value = null
    )

    val specification = Specification(
      id = "spec1",
      name = "Test Specification",
      givenStage = GivenStage(
        id = "given1",
        values = listOf(
          // Only non-event elements
          Command(
            id = "nonEvent",
            typeReference = CommandType(id = "nonEvent", name = "SomeElementNotEvent", schema = null),
            value = null
          )
        )
      ),
      whenStage = WhenStage(
        id = "when1",
        values = listOf(command)
      ),
      thenStage = ThenStage(
        id = "then1",
        values = listOf(resultEvent)
      )
    )

    // Verify the test method name
    assertThat(specification.testMethodName).isEqualTo("givenNoEvents_whenCommand1_thenEvent3")
  }

  @Test
  fun `test testMethodName requires exactly one command in when stage`() {
    // Create a specification with multiple commands in the when stage
    val command1 = Command(
      id = "command1",
      typeReference = CommandType(id = "command1", name = "Command1", schema = null),
      value = null
    )
    val command2 = Command(
      id = "command2",
      typeReference = CommandType(id = "command2", name = "Command2", schema = null),
      value = null
    )
    val resultEvent = Event(
      id = "event3",
      typeReference = EventType(id = "event3", name = "Event3", schema = null),
      value = null
    )

    val specification = Specification(
      id = "spec1",
      name = "Test Specification",
      givenStage = GivenStage(
        id = "given1",
        values = listOf()
      ),
      whenStage = WhenStage(
        id = "when1",
        values = listOf(command1, command2) // Multiple commands in when stage
      ),
      thenStage = ThenStage(
        id = "then1",
        values = listOf(resultEvent)
      )
    )

    // Verify that an IllegalArgumentException is thrown with the appropriate message
    assertThatThrownBy {
      specification.testMethodName
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Current implementation requires exactly one command in 'when' stage")
  }

}
