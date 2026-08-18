# Agent Instructions

Committed and public: never add production-system identifiers or evidence-corpus quotes here.

**This repository:** the shipping 1.x `jpa-metadata-maven-plugin` (JavaParser-based Maven Mojo
generating entity metamodels for Spring Data R2DBC), plus the paper design program for its v2
reboot as a JSR-269 annotation processor with an owned runtime library. No v2 code exists —
read the design documents before writing any.

## Hard rails — violations are one-way doors

1. **`_doc/` never enters git.** It is private working material and holds another party's
   proprietary source (`_doc/repo_usage/`, `_doc/repository/` — the evidence corpus). Never
   weaken the `.gitignore` entries; never quote its contents into committed files.
   Rationale: `_doc/adr/adr-corpus-restore-vs-reattest.md`.
2. **Never `git add .` or `git add -A`.** Stage explicit paths only.
3. **Push only via the SSH alias `github.com-vadimbabich`** (account isolation). Never point
   `origin` at a plain `github.com` URL.
4. **Golden corpus** (`src/it/simple-consumer/expected/**`): any change — including formatting
   and comments — requires an approving ADR that already exists and is **named in the commit
   message**, since the private ADRs cannot ride the same diff. Generated output must stay
   byte-deterministic: no dates, no environment-dependent content. Procedure:
   `docs/runbooks/golden-corpus-update.md`.
5. **Corpus-derived quantities keep their provenance marker** (`[review-time]` = provisional,
   or attested). Never restate them as verified facts. Procedure:
   `docs/runbooks/corpus-verification.md`.
6. **Never commit credentials, tokens or keys.** CI scans full history with gitleaks, but it
   only *detects*: a secret in a pushed commit is compromised and must be **rotated** —
   rewriting history does not undo it.

## Documents

`docs/` is committed and must be publishable as-is. `_doc/` is private and gitignored. Put new
files on the correct side.

- `_doc/hld-entity-metamodel.md` — the v2 HLD (§5 decision log, §8 open questions).
- `_doc/adr/` — twelve ADRs, one per HLD §8 question. Read the relevant one before proposing a
  decision; contradicting an ADR requires new evidence, not preference. Cite ADRs in commit
  messages by filename.
- `_doc/plans/`, `_doc/investigations/` — one file per workstream, WS-1 to WS-14.
- `docs/runbooks/` — corpus verification (G0), golden-corpus update, durability bundle (R-P0),
  ADR method. Follow them rather than re-deriving the procedures.
- `docs/reactive-code-style.md` — the reactive rule set (hand-written runtime, generated output,
  tests). Read it before writing or emitting a single reactive line; its §1 and §8 are contractual.

## Current state (2026-08-16)

- Shipping: the 1.x Mojo at `1.1.0-SNAPSHOT`; `master` is pushed to `origin`. Code lives in
  `src/main/java/io/github/vadimbabich/metadata/`: `parser/` (JavaParser) → `graph/` →
  `generator/` (JavaPoet), behind `api/`.
- v2 — annotation processor, runtime library, fluent query API — is designed on paper and
  gated. None of it is built.
- **All twelve ADRs are Proposed; none ratified.** Several propose revising things the HLD
  states as decided (generated-class naming default, an AOT claim, the "no Spring types in
  public signatures" rule, declaration-order determinism). Treat the HLD as the ratified record
  and the ADRs as proposals against it. Concretely: `pom.xml` enforces Maven `[3.9,)` while
  `_doc/adr/adr-maven-consumption-floor.md` proposes 3.6.3 — do not change the POM until it is
  ratified.
- Gates, in order: R-P0 (durability + the R-P0.1 publicness decision) → G0 (corpus closure) →
  naming/legal ADRs → model⇄contract⇄runtime review → D1 (generated-shape freeze) → combined
  spike → D2 (incremental category) → processor to parity → API freeze → 2.0.0.
- **`_doc/` survives only as local files** — in no commit and no bundle until
  `docs/runbooks/durability-bundle.md` step 3 runs.

## Branching and releases

- Work on `master` through short-lived `feature/YYYY.MM_short-desc` branches. Commit or push
  only when asked.
- **No long-lived `2.x` or `develop` branch.** v2 ships as milestones cut from `master`
  (`2.0.0-M1` → `-RC1` → `2.0.0`, per `_doc/adr/adr-initial-version-line.md`). A version branch
  is unworkable here: 1.x's remaining releases depend on v2 artifacts — runtime/core publishes
  at `2.0.0-M1` before the 1.x release that consumes it — and the golden corpus is the shared
  1.x⇄v2 parity contract.
- `release/2.0.x` or `1.1.x` branches on demand only, cut from a tag, then deleted.
- Releases are dispatch-triggered (`.github/workflows/release.yml`); milestones and RCs take the
  same path. Tag as `v<version>`, matching `v1.1.0` — the unprefixed `1.0.0` is a known
  inconsistency; do not add more.
- **Do not** split the single-module POM into parent + modules, and do not rename the repo or
  coordinates. The rename is decided in principle but WS-1 owns the deliverable; restructuring
  first means doing the same surgery twice.

## Code style

Optimize for scanning, not compactness.

- Explicit control flow over clever compression: no chained ternaries, no nested lambdas, no
  long `Optional` chains, no conditional embedded in a return expression. Stream pipelines are
  fine while they read as one idea — `stream().filter().map().collect()` in `parser/` is the
  house shape — but become a loop once a chain carries branching.
- Blank lines between logical steps. One responsibility per method, no boolean flag parameters.
- Names carry domain meaning (`normalizedKey`, `persistedEntity`), never `data`, `value`, `obj`,
  `result`. Never return `null`; prefer immutable collections and `List.of()`/`Map.of()`.
- Constructor injection, never field injection — except Mojo `@Parameter` fields, which Maven
  injects by design.
- `src/main/java` declares explicit types today (no `var`). Match the file you are editing
  rather than introducing a second style.

**Reactive code** — full rules in `docs/reactive-code-style.md`; read it before writing or emitting
reactive code. The non-negotiables, because they are the ones violated silently:

- **Never** block, schedule (`subscribeOn`/`publishOn`/`Schedulers`), `subscribe()`, or embed policy
  (`timeout`/`retryWhen`/`cache`/`onErrorContinue`) in library or generated code. Latency and retry
  budgets are the consumer's; blocking one event-loop thread stalls every request it multiplexes.
- **Defer every fallback that costs anything**: `switchIfEmpty(Mono.defer(...))` or
  `Mono.error(Supplier)` — the eager form builds the exception, stack trace and all, on the happy
  path too. Never `Mono.just(someCall())`.
- A named publisher is a *description*: two subscriptions run the I/O twice. Compose once.
- Bound `flatMap` concurrency against the connection pool; pick `concatMap` when order matters.
- Generated code is immutable, stateless, I/O-free and deterministic in emission order (rail 4).
- Tests assert signals with `StepVerifier`, including an error and a cancellation path; `block()`
  belongs in fixtures, never in an assertion.

**Comments and JavaDoc.** Comments explain *why*: a constraint, a workaround, a non-obvious
rule. Delete anything that restates the code. JavaDoc on public API only, one or two sentences,
never a restatement of the signature. One carve-out: JavaDoc on Mojo `@Parameter` fields is
harvested into `plugin.xml` by maven-plugin-plugin and *is* the `mvn help:describe` and site
documentation — keep it, however obvious it looks. Editing the generated-file header
(`FILE_HEADER`) changes generated output, so rail 4 applies.

## Build, test and quality gate — before every commit of code

Compiles at release 17 (`<java.release>`, enforcer floor `[17,)`) — write Java 17, not 21. CI
runs `mvn -B verify` on JDK 17, 21 and 25.

`mvn -B verify` runs the unit tests plus the invoker IT, which generates against
`src/it/simple-consumer` and byte-compares the golden corpus. Tests are JUnit 5 + AssertJ +
Mockito over fixtures in `src/test/resources/projects/simple-project`;
`GenerationReproducibilityTest` generates twice and asserts byte-identical, date-free output —
extend it when generator output changes.

1. Run IDE inspections on every file you changed (`mcp__idea__get_file_problems`, or
   `mcp__idea__lint_files` for a batch). Fix all errors, and all warnings unless you can state
   why the warning is wrong. Fix the cause — no `@SuppressWarnings` or other silencing.
2. Reformat only files you touched (`mcp__idea__reformat_file`); never bulk-reformat, and never
   reformat `src/it/**/expected/**` or generated sources (rail 4). Style is `.editorconfig`;
   change it deliberately, never as a side effect.
3. `mvn -B verify` must pass.
4. If the IDE MCP is unavailable, say so explicitly, fall back to `mvn -B verify` plus
   `mcp__ide__getDiagnostics`, and never report inspections as run when they were not.
