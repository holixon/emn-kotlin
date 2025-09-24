# EMN Kotlin - junie guidelines

## General

* we write kotlin code only, except for tests, where we might have an additional java test source folder for interop verification
* we prefer immutable state so whenever possible work with val and data classes
* we document architectural decisions in [`doc/adr`](../doc/adr) as markdown files. You must read and understand the existing ADRs and follow them.

## Testing

* we prefer assertj styled code over junit5, never use `assertEquals(1,3)`, instead use `assertThat(1).isEqualTo(3)`
