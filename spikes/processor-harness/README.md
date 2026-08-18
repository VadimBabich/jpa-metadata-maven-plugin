# Combined processor spike (WS-10)

**Throwaway evidence asset. Never published, never in the root reactor.** Version `0-SPIKE` exists so
that nothing can accidentally release it. The root `pom.xml` has packaging `maven-plugin` and no
`<modules>`, so `mvn -B verify` at the repository root does not see this directory.

Build it on its own:

```bash
mvn -B -f spikes/processor-harness/pom.xml verify
```

## Why this exists

One minimal JSR-269 processor answers three otherwise-separate program questions, so each is answered
once, by one asset:

1. **Parity** — can a JSR-269 processor reproduce a committed golden file byte-for-byte, and does the
   test harness survive JDK 17/21/25?
2. **Broken sources** — what does a processor observe when compilation is already failing, which is
   the normal case inside an IDE?
3. **Staleness** — under Gradle incremental annotation processing, does an `isolating` processor leave
   a stale-but-compiling generated file when a *referenced* entity changes?

## What it is not

- Not a real processor: no owned metadata model, no JavaPoet, no options surface, no naming strategy.
  The emitted text is a fixed template, not a design.
- **Reproducing the 1.x shape is not an endorsement of it.** The golden corpus is a pre-D1 regression
  baseline; its known defects are listed in `src/it/simple-consumer/expected/README.md`. D1 freezes the
  shape, and D1 has not happened.
- `@SpikeReferences` is deliberately *not* named `@References`. The relationship vocabulary — name,
  retention, coordinates — is an open decision that this asset must not pre-empt.

## Relationship to the golden corpus

The spike **reads** `src/it/simple-consumer/` and never writes it. Entity sources and golden files are
referenced in place rather than copied, so there is no second copy that can silently diverge. The
corpus remains the single authority for what the 1.x shape is.

Constant naming reproduces `R2dbcEntityMetadataGenerator#toConstantName` exactly; byte parity depends
on it.
