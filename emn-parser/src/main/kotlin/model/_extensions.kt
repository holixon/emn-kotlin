package io.holixon.emn.model

import io.holixon.emn.model.FlowElement.FlowNode.*

fun List<FlowElement>.commands() = filterIsInstance<Command>()
fun List<FlowElement>.events() = filterIsInstance<Event>()
fun List<FlowElement>.views() = filterIsInstance<View>()
fun List<FlowElement>.queries() = filterIsInstance<Query>()

fun Command.possibleEvents() = this.outgoing.map { flow -> flow.target }.events()

fun Command.views() = this.incoming.map { flow -> flow.source }.views()

fun View.queries() = this.incoming.map { flow -> flow.source }.queries()

fun Query.events() = this.incoming.map { flow -> flow.source }.events()

fun Event.commands() = this.incoming.map { flow -> flow.source }.commands()
