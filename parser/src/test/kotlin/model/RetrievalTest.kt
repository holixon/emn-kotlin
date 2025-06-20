package model

import io.holixon.emn.model.EmnDocumentParser
import io.holixon.emn.model.FlowElement
import io.holixon.emn.model.FlowElementType
import org.junit.jupiter.api.Test
import java.io.File

class RetrievalTest {

  private val parser = EmnDocumentParser()

  @Test
  fun retrieves_for_command_handlers() {
    val file = File("src/test/resources/faculty.emn")
    val result = parser.parseDefinitions(file)


    result.nodeTypes.filterIsInstance<FlowElementType.FlowNodeType.CommandType>().forEach { commandType ->

      println(commandType)
      // -> generate Command class

      result.timelines.map { timeline ->

        val commands = timeline.flowElements.filterIsInstance<FlowElement.FlowNode.Command>().filter { it.typeReference == commandType }

        commands.forEach { c ->

          println(c)

          val views = c.incoming.map { incoming -> incoming.source }.filterIsInstance<FlowElement.FlowNode.View>()
          views.forEach { view -> println(view) }
          val events = views.map { view -> view.incoming.map { incoming -> incoming.source } }.flatten()

          events.forEach { event -> println(event) }
        }
      }

    }

  }

}
