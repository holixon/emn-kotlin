package io.holixon.emn.dom4j


val EMN_NAMESPACES: Array<String> = arrayOf(
  "https://holixon.io/spec/EMN/20241231/MODEL"
)


object ElementNames {

  const val DEFINITIONS = "definitions"

  const val TYPES = "types"

  const val VIEW_TYPE = "viewType"
  const val COMMAND_TYPE = "commandType"
  const val EVENT_TYPE = "eventType"
  const val QUERY_TYPE = "queryType"
  const val ERROR_TYPE = "errorType"
  const val EXTERNAL_EVENT_TYPE = "externalEventType"
  const val EXTERNAL_SYSTEM_TYPE = "externalSystemType"
  const val INFORMATION_FLOW_TYPE = "informationFlowType"
  const val TRANSLATION_TYPE = "translationType"
  const val AUTOMATION_TYPE = "automationType"

  const val SPECIFICATION = "specification"
  const val STAGE_GIVEN = "given"
  const val STAGE_WHEN = "when"
  const val STAGE_THEN = "then"

  const val TIMELINE = "timeline"

  const val LANE_SET = "laneSet"
  const val TRIGGER_LANE_SET = "triggerLaneSet"
  const val TRIGGER_LANE = "triggerLane"
  const val CONCEPT_LANE_SET = "conceptLaneSet"
  const val CONCEPT_LANE = "conceptLane"

  const val SLICE_SET = "sliceSet"
  const val SLICE = "slice"

  const val AUTOMATION = "automation"
  const val COMMAND = "command"
  const val ERROR = "error"
  const val EVENT = "event"
  const val EXTERNAL_EVENT = "externalEvent"
  const val EXTERNAL_SYSTEM = "externalSystem"
  const val INFORMATION_FLOW = "informationFlow"
  const val FLOW_NODE_REF = "flowNodeRef"
  const val QUERY = "query"
  const val TRANSLATION = "translation"
  const val VIEW = "view"

  const val SCHEMA = "schema"
  const val ID_SCHEMA = "idSchema"
  const val VALUE = "value"
}

object AttributeNames {
  const val SCENARIO = "scenario"
  const val SLICE_REF = "sliceRef"
  const val STATE_NAME = "stateName"
  const val SCHEMA_FORMAT = "schemaFormat"
  const val RESOURCE = "resource"
  const val SOURCE_REF = "sourceRef"
  const val TARGET_REF = "targetRef"
  const val ID = "id"
  const val NAME = "name"
  const val TYPE_REF = "typeRef"
  const val VALUE_FORMAT = "valueFormat"
}

object Defaults {
  const val APPLICATION_JSON = "application/json"
  const val APPLICATION_JSON_SCHEMA = "application/json-schema"
}
