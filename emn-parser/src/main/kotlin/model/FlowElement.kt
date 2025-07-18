package io.holixon.emn.model

import io.holixon.emn.model.FlowElementType.FlowNodeType
import io.holixon.emn.model.FlowElementType.FlowNodeType.*
import io.holixon.emn.model.FlowElementType.MessageFlowType

sealed class FlowElement(
    open val id: String,
    open val typeReference: FlowElementType,
    open val example: ExampleValue?,
) {

    data class MessageFlow(
        override val id: String,
        override val typeReference: MessageFlowType,
        val source: FlowNode,
        val target: FlowNode
    ) : FlowElement(id = id, typeReference = typeReference, example = null)

    sealed class FlowNode(
        override val id: String,
        override val typeReference: FlowNodeType,
        override val example: ExampleValue?,
        open val incoming: MutableList<MessageFlow> = mutableListOf(),
        open val outgoing: MutableList<MessageFlow> = mutableListOf(),
    ) : FlowElement(id = id, typeReference = typeReference, example = example) {

        class Command(id: String, typeReference: CommandType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example) {

            fun sourcedEvents(): List<Event> {
                val directEvents = this.views()
                    .flatMap { view -> view.queries() }
                    .map { query -> query.events() }
                    .flatten()
                return directEvents + directEvents
                    .filterNot { directEvents.contains(it) }
                    .map { event ->
                        event.commands().filterNot { it == this }
                            .map { it.sourcedEvents() }.flatten()
                    }.flatten()
            }

            fun possibleEvents() = this.outgoing.map { flow -> flow.target }.events()
            fun views() = this.incoming.map { flow -> flow.source }.views()
        }

        class Query(id: String, typeReference: QueryType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example) {
            fun events() = this.incoming.map { flow -> flow.source }.events()
        }

        class Event(id: String, typeReference: EventType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example) {
            fun commands() = this.incoming.map { flow -> flow.source }.commands()
        }

        class ExternalEvent(id: String, typeReference: ExternalEventType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example)

        class View(id: String, typeReference: ViewType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example) {
            fun queries() = this.incoming.map { flow -> flow.source }.queries()
        }

        class Translation(id: String, typeReference: TranslationType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example)

        class Automation(id: String, typeReference: AutomationType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example)

        class ExternalSystem(id: String, typeReference: ExternalSystemType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example)

        class Error(id: String, typeReference: ErrorType, example: ExampleValue?) :
            FlowNode(id = id, typeReference = typeReference, example = example)

        class FlowNodeReference(id: String) : FlowNode(id = id, typeReference = NoTypeReference, example = null)
    }

    override fun toString(): String =
        "${this::class.simpleName}(id=$id, name=${typeReference.name}, type=${typeReference.id}, example=${example})"

}