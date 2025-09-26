# EMN Kotlin - junie guidelines

## General

* we write kotlin code only, except for tests, where we might have an additional java test source folder for interop verification
* we prefer immutable state so whenever possible work with val and data classes
* we document architectural decisions in [`doc/adr`](../doc/adr) as markdown files. You must read and understand the existing ADRs and follow them.

## Code Style Conventions

* we follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
* Very important: You **must** respect the directory structure convention:
  * The package name of a kotlin file in the root source folder `src/[main|test]/kotlin` defines the root package of the module
  * All other package are relative to that root package
  * Example:
    * `src/main/kotlin/Foo.kt` -> package `io.acme.myproject`
    * `src/main/kotlin/bar/Foo.kt` -> package `io.acme.myproject.bar`

## Testing

* we prefer assertj styled code over junit5, never use `assertEquals(1,3)`, instead use `assertThat(1).isEqualTo(3)`

## Tools

### Implement the concreteType State for a generated commandHandler

For our vertical slice based command handlers, we generate the state interface via maven-plugin, using the EMN and AVRO
declarations. These state interfaces can be identified easily because:

* they are annotated with `@EventSourcedEntity`
* they are inner classes of a class named `*CommandHandler`
* they declare methods named `decide` and `evolve`

The CommandHandler class is generated as well, such as a test method that uses the Axon Test Fixture and given/when/then
tests to verify the correct behaviour.

Whenever we ask to implement the concreteType State, we need to follow these guidelines:

* Identify the correct CommandHandler class and the inner State interface
* Find the matching generated CommandHandlerTest, which contains the test cases for the CommandHandler
* If no test class can be found for a handler, skip the following steps for generation
* Find the generated Concrete state implementation of that State interface (which `evolve` and `decide` methods are marked as `TODO()`)
* Implement the `decide` methods and all required `evolve` methods so the CommandHandlerTest passes
  * Never change the CommandHandlerTest, only change the concrete State implementation
  * Adding mutable state variables is ok
* If the implementation is complete, ask for a code review, and then move the classes to there correct package location in the `src/main/kotlin` directory
