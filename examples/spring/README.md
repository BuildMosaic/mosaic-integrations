# Spring example

An ordinary Spring service reaches a framework-independent Tile through a single
Canvas resolver. Spring owns the Canvas bean and closes it when the context exits.

From the repository root with JDK 21:

```sh
./gradlew :examples:spring:run --no-daemon
```

Expected output: `Hello from Spring`. The executable checks this result and runs
as part of `build`. See the [adapter guide](../../mosaic-spring/README.md) for
qualifiers, ownership, and scope boundaries.
