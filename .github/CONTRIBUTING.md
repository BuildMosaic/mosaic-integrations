# Contributing to Mosaic Integrations

Thanks for your interest in contributing! 🎉

## How to contribute

- **Issues:** Report bugs with a reproduction and the Mosaic/framework versions.
  Propose integrations around a real application problem and the manual wiring
  they would remove. Discuss consequential APIs and ownership changes before
  implementing them.
- **Pull requests:** Fork the repository, create a feature branch, and submit a
  PR against `main`. Link relevant issues and explain the resulting behavior.
- **Code style:** Follow `.editorconfig` and nearby code. Use clear names and
  small, descriptive commits with imperative titles.
- **Tests:** Add coverage at the public integration boundary. Use the real
  framework for lifecycle, DI, tracing, and container behavior where practical.
  Document the version combinations exercised and any verification limits.

## Development setup

Use JDK 21 and the checked-in [Gradle wrapper](../gradle/wrapper/gradle-wrapper.properties):

```bash
./gradlew clean build --no-daemon
git diff --check
```

CI runs these commands on pushes and pull requests to `main`. The root build
provides the Gradle lifecycle and dependency catalog. Adapter modules configure
their own dependencies and behavioral tests.

The [version catalog](../gradle/libs.versions.toml) defines Kotlin, Mosaic, and
framework dependency versions. When adding a library, configure a JDK 21 toolchain
and Kotlin JVM target plus Java source/target compatibility of 17, unless the
integration requires a stricter minimum. Include real modules in
`settings.gradle.kts`. Maven Central is the normal dependency repository. No local
core checkout or Maven Local installation is required.

## Integration design

Adapters should reuse the application's existing infrastructure through public
framework and Mosaic APIs. Keep framework-managed dependencies, lifecycle, and
telemetry framework-owned. Mosaic core remains responsible for runtime execution
and Canvas behavior. Surface missing core extension points rather than duplicating
runtime behavior or accessing internals.

Each adapter needs explicit prerequisites, supported Mosaic/framework versions,
setup, ownership and request-boundary behavior, and limitations. Runnable framework
applications live in [Mosaic](https://github.com/BuildMosaic/Mosaic/tree/main/examples);
update those canonical examples after adapters are published. Shared abstractions
need evidence from multiple real integrations.
Artifact names, release versioning, and publication policy are not yet decided.

See [AGENTS.md](../AGENTS.md) for repository boundaries and documentation skills,
and follow the [Code of Conduct](CODE_OF_CONDUCT.md). Use
[GitHub Issues](https://github.com/BuildMosaic/mosaic-integrations/issues) for
questions and proposals; report vulnerabilities through the
[security policy](SECURITY.md).
