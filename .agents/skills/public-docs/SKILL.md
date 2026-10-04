---
name: public-docs
description: Write or edit Mosaic Integrations' root README, overview docs, integration summaries, installation and quick-start copy, and comparisons for developers evaluating adoption. Use technical-docs for learning, tasks, reference, and concepts.
---

# Public documentation

Treat the README as a product page for developers, not a project diary or internal
wiki. Help readers understand which official integrations are available, what manual
application wiring each saves, its limitations, and where to learn more. Factual
marketing is welcome; every claim must hold up against repository evidence.

Route by reader purpose: this skill owns adoption, positioning, overview, first
impressions, and public proof. Use [technical-docs](../technical-docs/SKILL.md) for
learning, completing tasks, lookup, or understanding concepts and internals.
For changes spanning both audiences, apply each skill to the relevant document
or section; the root README demonstrates capabilities and links to deeper material.

## Decide before drafting

1. Read applicable AGENTS.md instructions and the entire target document.
   Inspect heading order, narrative flow, tone, emoji/icons, example style,
   terminology, and depth. For a new page, read its parent and closest peers.
2. Read nearby public docs and follow relevant links to see what is already
   explained elsewhere. Check the supporting code, tests, build configuration,
   or results before making claims.
3. Identify the intended reader and the decision or task this addition supports.
   Decide whether it belongs in the root README at all, an existing deeper page,
   or maintainer material. Choose the location before writing prose.
4. Read the headings and content immediately before and after the proposed
   insertion. Does the sequence still tell a coherent story? Avoid interrupting
   related sections; never append a section just because a task produced facts.
5. Make the smallest useful edit. Restructure more broadly only when the existing
   information architecture is itself the problem, not because one section needs
   work. Do not replace an established README with a generic template.
6. Reread the entire document after editing. Check flow, repetition, links,
   factual support, and whether the diff stays within the requested scope.

## Preserve showcase value

The root README is also a showcase, not merely an index of deeper documentation.
Before large cuts, inventory the integrations actually implemented and published
here. Keep their concrete application benefits and supported combinations
discoverable. Never imply an integration exists, is available, or is supported
before implementation and publication. Do not turn likely future frameworks into
an aspirational feature list or fabricate installation coordinates.

When an integration exists, show the developer-facing API and explain the manual
DI, lifecycle, or tracing wiring it removes. Link to its canonical framework guide.
While none exists, an honest, small overview is the complete product page.

Shorter is not inherently better. Use the shortest page that still makes the
project understandable, differentiated, and credible. Keep product proof visible:
runnable examples, supported versions, and meaningful integration tests should not
sit several clicks away when they materially support adoption.
For major rewrites, reread both versions and ask whether the new one is more
accurate but materially less compelling. Correctness is mandatory; preserve enough
concrete evidence to make an evaluating developer want to investigate further.

## Placement and Mosaic's house style

Public/supported status alone does not earn an API README prominence. Follow the
user-facing abstraction hierarchy: showcase the consumer capability, and keep
low-level extension or integration-author SPIs in KDoc or dedicated author docs
unless ordinary users need them directly. READMEs are product/usage pages, not
catalogs of every supported API. A future higher-level consumer API should be
introduced through that API, not by promoting its low-level installation plumbing.

Use progressive disclosure: demonstrate, then link. Give a representative concrete
example or proof before sending the reader to the canonical guide for depth. Do
not replace every useful example with a link or duplicate full walkthroughs.

Follow the [root README](../../../README.md) and each target's house style: direct
developer value, short paragraphs, Mosaic terminology, and concrete Kotlin
examples when backed by real code. Do not impose a generic template, mandatory
emoji, or an API inventory.

Keep installation and first-use copy focused on getting started. Put supported
Mosaic/framework versions and complete setup in the canonical framework-specific
guide. Link to [Mosaic](https://github.com/Nick-Abbott/Mosaic) and
[buildmosaic.org](https://buildmosaic.org) for core concepts rather than repeating
core documentation. Explain that integrations reuse application infrastructure
and preserve framework ownership; Mosaic core owns execution and Canvas behavior.

Write evergreen documentation as a description of how Mosaic works now. Avoid
“introduced in version X,” “previously,” “current development,” PR chronology,
migration history, and before/after narration. Put release and change history in
changelogs or release notes. Include exact dependency, toolchain, or library
versions only when needed for installation, compatibility, configuration, or
operation. In broader docs, link to the canonical compatibility or setup page
rather than repeating its version constraints.

Prefer concrete claims, short paragraphs, useful examples, clear hierarchy, and
Mosaic's existing terms (Canvas, Mosaic, Tile, MultiTile). Lead with user value.
Avoid generic AI prose, unsupported adjectives, repeated explanations, and raw
benchmark, test, or tool-output dumps.

Explain developer value directly before discussing where an example came from or
how it was validated; avoid provenance and meta-documentation as landing-page copy.
When showcasing generated tooling, choose representative output that makes its
benefit visible. Keep error-heavy or pathological diagnostics in deeper docs unless
the limitation itself is central to adoption.

## Evidence and audience boundaries

Keep demonstrated behavior, interpretation, and public summary distinguishable.
Claims about less configuration, lifecycle safety, tracing reuse, or compatibility
need support in the implementation, runnable examples, and real-framework tests.
Preserve the evidence's scope and limitations. A successful build with one version
is not proof of a broad framework compatibility range.

Normally omit worktree details, CI/debugging history, failed experiments,
rejected tools, local paths, workstation setup,
internal review process, temporary implementation constraints, and task/PR
chronology. Keep these in PR history, issues, developer/maintenance docs, or
generated local reports unless a library user genuinely needs them. Include
current user-relevant limitations and tradeoffs without narrating how they arose.
