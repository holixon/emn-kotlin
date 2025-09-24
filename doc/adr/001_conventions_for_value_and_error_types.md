# ADR-001: Conventions for Value and Error Types

We generate code based on Avro schemas, which are grouped via Avro protocol types (`.avpr` files). We do not use
the protocol messages, but rather the types defined in the protocol, because avro discourages re-defining types in
different files, which we need, especially for common values such as value types (e.g. Entity Ids) and error types (e.g. Domain exceptions).

Because we control the generation, we can enforce some conventions on how these types are defined and used.

## Value Types

Value types (mainly used for Entity Ids) are of avro type `record` and have exactly one field, named `value`, 
which preferably of type `string` or `long`. Custom types as UUID are also possible, but currently not supported.

**Benefit**: Whenever we need to read or write a value type, we can rely on the single field `value`.

**Example**:

```json
{
  "type": "record",
  "name": "CourseId",
  "namespace": "io.holixon.emn.example.faculty",
  "fields": [
    {
      "name": "value",
      "type": "string"
    }
  ]
}
```

will generate a class `CourseId` with a single property `value` of type `String`.

```kotlin

/** @param value Constructor property for field 'value' of record type 'CourseId'. */
@Serializable
public data class CourseId(
  /** Constructor property for field 'value' of record type 'CourseId'. */
  public val `value`: String
) {
  public companion object {
    public const val ENTITY_ID: String = "COURSE"

    public fun random(): CourseId = CourseId("${ENTITY_ID}:${randomUUID()}")
  }
}
```

## Error Types

Error types (mainly used for Domain exceptions) are of avro type `error` and have exactly one field, named `message` of type `string`,
representing the exception message.

**Benefit**: Whenever we need to read or write an error type, we can rely on the single field `message`.

**Example**:

```json
{
      "type": "error",
      "name": "DuplicateCourse",
      "doc": "Error: A course name has to be unique.",
      "namespace": "io.holixon.emn.example.faculty",
      "fields": [
        {
          "name": "message",
          "type": "string"
        }
      ]
    }
```

will generate a class `DuplicateCourse` with a single property `message` of type `String`.

```kotlin

/** Error: A course name has to be unique. */
public class DuplicateCourse(message: String) : AvroRemoteException(message)

```
