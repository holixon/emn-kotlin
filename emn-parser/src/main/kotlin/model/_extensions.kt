package io.holixon.emn.model

fun List<FlowElement>.commands() = filterIsInstance<Command>()
fun List<FlowElement>.events() = filterIsInstance<Event>()
fun List<FlowElement>.views() = filterIsInstance<View>()
fun List<FlowElement>.queries() = filterIsInstance<Query>()
