---
name: technical-docs
description: Write or edit Mosaic integration guides, tutorials, how-tos, reference, and ownership or architecture explanations under docs/ and in module documentation. Use public-docs for adoption, positioning, project overviews, and public result summaries.
---

# Technical documentation

Start with the reader's intent and choose a home for the information before
drafting. This skill owns learning, performing a task, looking up facts, and
understanding concepts or internals, including module READMEs serving those goals.
Use [public-docs](../public-docs/SKILL.md) for adoption, positioning, first
impressions, and public proof, even when that content lives outside the root
README. Apply both skills to changes spanning both purposes, keeping each
document appropriate to its audience.

## Classify the content

Choose the primary purpose before writing; these are reader needs, not required
directory names or a reason to reorganize existing docs.

| Purpose | Reader intent | Writing approach |
| --- | --- | --- |
| Tutorial | Learn an integration by following a guided path. | Give one clear path with prerequisites, working examples, and visible outcomes. |
| How-to | Accomplish a known task. | Start with the goal, give direct steps, and show how to verify success. |
| Reference | Look up precise facts. | Be terse and complete for the stated scope: APIs, options, defaults, constraints, commands. |
| Explanation | Understand concepts, design, or tradeoffs. | Explain why and how ideas connect; keep rationale here rather than inside procedural steps. |

Avoid mixing these styles unnecessarily. Link to supporting reference or
explanation instead of turning a tutorial into an exhaustive manual. An existing
mixed guide can keep distinct sections; a small addition does not require a split.

## Find the narrowest useful home

Supported/public API does not automatically belong in a README. Documentation
prominence should follow the user-facing abstraction hierarchy. Keep low-level
extension and integration-author SPIs in API KDoc or dedicated integration-author
docs unless ordinary application developers need them directly. READMEs should
remain compelling product and usage pages, not catalogs of every supported API;
lead with the implemented higher-level consumer API when one exists. Do not
present planned APIs as available.

1. Read applicable AGENTS.md instructions, the root README for orientation, the
   entire target document, and relevant neighboring module READMEs and docs.
   Notice their structure, naming, links, terminology, tone, and example style.
2. Identify the audience, what they already know, and what they need to learn,
   do, look up, or understand. Distinguish library users from maintainers.
3. Search existing documentation for the concept and related API names. Extend
   an existing section when it meets the need; avoid duplicate explanations.
4. Before creating a page or section, answer: “Why does this belong here rather
   than in README.md or another existing page?” Preserve current organization
   and naming; do not impose a new documentation taxonomy or map.
5. Check adjacent headings for a coherent sequence. Add cross-links where they
   materially improve discovery, keeping a brief summary at broader entry points.

Keep framework setup and behavior in the integration's canonical guide. Link to
[Mosaic's core guide](https://github.com/BuildMosaic/Mosaic/blob/main/mosaic-core/README.md)
for composition and Canvas concepts and
[Mosaic's test guide](https://github.com/BuildMosaic/Mosaic/blob/main/mosaic-test/README.md)
for core testing support. Do not duplicate core documentation or create empty
guides for integrations that do not exist.

## Cover the integration contract

Choose the relevant detail for the reader's task, keeping a canonical home for:

- prerequisites, supported Mosaic/framework versions, and Java/Kotlin requirements;
- published installation coordinates and required framework setup;
- DI behavior, scope, qualifiers, and unsupported resolution behavior;
- lifecycle and resource ownership, including who creates and closes resources;
- request boundaries and their relationship to Mosaic execution;
- tracing behavior and reuse of framework-provided telemetry infrastructure;
- configuration, defaults, limitations, and observable failure behavior;
- complete runnable examples and commands to verify their results.

Keep build JDK/toolchain requirements distinct from library runtime minimums.
Make unsupported combinations explicit. If a missing core extension point blocks
behavior, describe the requirement honestly rather than documenting an internal
workaround as supported integration behavior.

## Write and verify

Make the smallest edit that serves the chosen purpose. Match the target's style
and Mosaic terminology, use short paragraphs and useful examples, and link to
deeper material instead of repeating it. Keep maintainer/process details in
maintenance documentation and out of user guides unless needed for the task.

Write evergreen documentation as a description of how Mosaic works now. Avoid
“introduced in version X,” “previously,” “current development,” PR chronology,
migration history, and before/after narration. Put release and change history in
changelogs or release notes. Include exact dependency, toolchain, or library
versions only when needed for installation, compatibility, configuration, or
operation. In broader docs, link to the canonical compatibility or setup page
rather than repeating its version constraints.

Verify code, API names, commands, versions, defaults, and constraints against
source, tests, examples, and build configuration. Examples should compile or
faithfully reflect the current API with necessary context made clear. Verify
lifecycle, DI, tracing, request boundaries, and container behavior against
the real framework where practical. Run changed runnable examples and supported
compatibility checks; report verification limits honestly. Do not substitute mock
behavior for evidence of framework behavior or claim untested version support.

Reread the complete edited page for reader intent, placement, flow, and duplicated
content. Resolve added or changed links and anchors, review the diff for unrelated
rewrites, and run `git diff --check`.
