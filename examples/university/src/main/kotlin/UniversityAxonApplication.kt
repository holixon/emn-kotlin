package io.holixon.emn.example.university


import io.holixon.emn.example.university.faculty.configureFacultyModule
import io.holixon.emn.example.university.faculty.type.course.CourseId
import io.holixon.emn.example.university.faculty.write.createcourse.CreateCourse
import io.holixon.emn.example.university.faculty.write.renamecourse.RenameCourse
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.common.infra.FilesystemStyleComponentDescriptor
import org.axonframework.configuration.ApplicationConfigurer
import org.axonframework.configuration.AxonConfiguration
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import java.util.logging.Logger

class UniversityAxonApplication {

    companion object {

      private val logger: Logger = Logger.getLogger(UniversityAxonApplication::class.java.name)

        @JvmStatic
        fun main(args: Array<String>) {
            val configuration: AxonConfiguration = startApplication()
            printApplicationConfiguration(configuration)
            executeSampleCommands(configuration)
        }

        private fun startApplication(): AxonConfiguration {
            val configurer: ApplicationConfigurer = UniversityAxonApplication().configurer()
            return configurer.start()
        }

        private fun printApplicationConfiguration(configuration: AxonConfiguration) {
            val componentDescriptor = FilesystemStyleComponentDescriptor()
            componentDescriptor.describeProperty("configuration", configuration)
            logger.info(
                """
                    Application started with following configuration:
                    ${componentDescriptor.describe()}
                    """.trimIndent()
            )
        }

        private fun executeSampleCommands(configuration: AxonConfiguration) {
            val courseId = CourseId.random()
            val createCourse = CreateCourse(courseId, "Event Sourcing in Practice", 3)
            val renameCourse = RenameCourse(courseId, "Advanced Event Sourcing")

            val commandGateway: CommandGateway = configuration.getComponent(CommandGateway::class.java)
            commandGateway.sendAndWait(createCourse)
            commandGateway.sendAndWait(renameCourse)
        }
    }


  fun configurer(): ApplicationConfigurer {
    return EventSourcingConfigurer
      .create()
      .configureFacultyModule()
  }

}
