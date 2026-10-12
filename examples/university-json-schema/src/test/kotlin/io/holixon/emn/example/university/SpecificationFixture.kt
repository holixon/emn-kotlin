package io.holixon.emn.example.university

import io.holixon.emn.example.university.command.CommandType_create_course
import io.holixon.emn.example.university.command.CommandType_rename_course
import io.holixon.emn.example.university.event.EventType_course_created
import io.holixon.emn.example.university.event.EventType_course_renamed
import org.axonframework.messaging.commandhandling.CommandHandler
import org.axonframework.messaging.commandhandling.CommandResultMessage
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.messaging.core.GenericMessage
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.core.QualifiedName
import org.axonframework.messaging.core.configuration.MessagingConfigurer
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventhandling.EventSink
import org.axonframework.messaging.eventhandling.GenericEventMessage
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.test.extension.AxonTestFixtureProvider
import org.axonframework.test.fixture.AxonTestFixture

class UniversityFixtureProvider : AxonTestFixtureProvider {

  override fun get(): AxonTestFixture {
    val courses = CourseState()
    val commandModule = CommandHandlingModule.named("course-specifications")
      .commandHandlers()
      .commandHandler(
        QualifiedName(CommandType_create_course::class.java),
        commandHandler { configuration, command, context ->
          val payload = command.payload() as CommandType_create_course
          if (courses.contains(payload.courseId)) {
            throw ErrorType_duplicate_course("Course with id=${payload.courseId} already exists.")
          }
          configuration.getComponent(EventSink::class.java).publish(
            context,
            eventMessage(EventType_course_created(payload.courseId, payload.name, payload.capacity))
          )
        }
      )
      .commandHandler(
        QualifiedName(CommandType_rename_course::class.java),
        commandHandler { configuration, command, context ->
          val payload = command.payload() as CommandType_rename_course
          val existingName = courses.nameOf(payload.courseId)
            ?: throw ErrorType_course_does_not_exist("Course with id=${payload.courseId} does not exist.")
          if (existingName != payload.name) {
            configuration.getComponent(EventSink::class.java).publish(
              context,
              eventMessage(EventType_course_renamed(payload.courseId, payload.name))
            )
          }
        }
      )

    val configurer = MessagingConfigurer.create()
      .registerCommandHandlingModule(commandModule)
      .eventProcessing { eventProcessing ->
        eventProcessing.subscribing { subscribing ->
          subscribing.defaultProcessor("course-state") { components ->
            components.autodetected("course-state") { courses }
          }
        }
      }
    return AxonTestFixture.with(configurer)
  }

  private fun commandHandler(
    handler: (org.axonframework.common.configuration.Configuration,
              org.axonframework.messaging.commandhandling.CommandMessage,
              org.axonframework.messaging.core.unitofwork.ProcessingContext) -> Unit
  ): org.axonframework.common.configuration.ComponentBuilder<CommandHandler> =
    org.axonframework.common.configuration.ComponentBuilder { configuration ->
      CommandHandler { command, context ->
        handler(configuration, command, context)
        MessageStream.empty<CommandResultMessage>()
      }
    }

  private fun eventMessage(payload: Any): EventMessage =
    GenericEventMessage(GenericMessage(MessageType(payload.javaClass), payload))
}

private class CourseState {
  private val courses = mutableMapOf<String, String>()

  fun contains(courseId: String): Boolean = courses.containsKey(courseId)

  fun nameOf(courseId: String): String? = courses[courseId]

  @EventHandler
  fun on(event: EventType_course_created) {
    courses[event.courseId] = event.name
  }

  @EventHandler
  fun on(event: EventType_course_renamed) {
    courses[event.courseId] = event.name
  }
}

private class ErrorType_duplicate_course(message: String) : RuntimeException(message)

private class ErrorType_course_does_not_exist(message: String) : RuntimeException(message)
