# Repository guide

Mosaic Integrations owns first-party adapters between Mosaic and external
application frameworks/platforms, along with their documentation, runnable
examples, and compatibility tests. No adapters are implemented or published yet.

## Integration boundaries

- Keep adapters thin translations over public framework and Mosaic APIs/SPIs.
  Mosaic core owns Tile execution, caching, batching, deduplication, cancellation
  semantics, Canvas behavior, and execution observation. Do not duplicate them.
- Reuse the application's framework infrastructure. Framework DI, lifecycle,
  telemetry, and resources remain framework-owned unless an API explicitly
  transfers ownership. Make request boundaries and cleanup responsibilities clear.
- Keep unsupported framework behavior explicit. Do not hide it with reflection,
  internal APIs, silent fallbacks, or implicit configuration magic.
- Consume released Mosaic artifacts from Maven Central. Do not introduce source
  linkage, an implicit composite build against core, or `mavenLocal()` in normal
  dependency resolution.
- If an adapter needs a missing core extension point, describe the required public
  behavior and surface it as a Mosaic core API requirement. Do not work around it
  with internal access, duplicated runtime behavior, or hidden coupling.
- Supported Mosaic/framework version combinations are part of each adapter's
  public contract. Assess Kotlin source, JVM binary, and framework compatibility.
- Shared abstractions should emerge from multiple real integrations. Do not
  create a generic integration framework for one adapter or a hypothetical one.

## Build baseline

Use Gradle 8.14.4 with build JDK and toolchain 21. Future Kotlin libraries use
Kotlin 2.4.20 and JVM target/minimum 17 unless an actual integration requires a
stricter target. Configure both Kotlin JVM target and Java source/target
compatibility when adding a library. The version catalog records released Mosaic
0.6.0; it is a dependency version, not this repository's release version.

Add real modules to `settings.gradle.kts` when implemented. There are no module
conventions, formatting/static-analysis plugins, or publishing tasks yet. Add
build logic only when it has work to do. Artifact naming, versioning, compatibility
policy, and release cadence remain undecided.

## Documentation skills

- For adoption-focused README/project docs and public summaries, read
  [public-docs](.agents/skills/public-docs/SKILL.md).
- For framework guides, reference, and explanations, read
  [technical-docs](.agents/skills/technical-docs/SKILL.md). Route by reader purpose;
  use both when a change spans both audiences.
- Link to canonical Mosaic documentation for core concepts. Never present a
  planned integration as implemented, published, or supported.

## Architecture skills

- When designing or materially restructuring an adapter, public interface, or
  ownership model, use [codebase-design](.agents/skills/codebase-design/SKILL.md).
- After a substantial feature/refactor is behaviorally correct, use
  [zero-tech-debt](.agents/skills/zero-tech-debt/SKILL.md) to review its final shape.

Neither skill is required for routine edits or authorizes unrelated cleanup.

## Tests and validation

Use the cheapest test layer that decisively proves the behavior. Prefer actual
framework integration tests over mocks for lifecycle, DI, tracing, and container
behavior. Exercise supported versions and resource ownership at the public
boundary. Keep examples complete and runnable; do not add fake modules or tests.

Run before finalizing:

```bash
./gradlew clean build --no-daemon
git diff --check
```

Run any additional formatting/static checks and relevant framework compatibility
or example checks added by the change. Validate wrapper changes against official
Gradle checksums, check workflow YAML, and verify changed documentation links.

Use a sibling worktree for substantial changes. Do not modify another active
worktree or remove it; remove only the clean worktree created for your task.
