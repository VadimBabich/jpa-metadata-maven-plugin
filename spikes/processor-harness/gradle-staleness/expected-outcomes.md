# G3 staleness experiment — pre-registered outcome classification

Written **before** any run, so results are classified, not rationalized (plan `07-ws10` TDD
strategy). Scenario verbatim from investigation 05-ws6 §E4: fixtures `E --@SpikeReferences--> X`;
full build; then, without cleaning, (a) rename `X`'s `@Id` property, (b) change its type
`Long`→`String`, (c) delete `X.java`; under isolating and aggregating registration.

## Classification taxonomy (E4's decision form)

- **regenerated-correct** — `E_` regenerated with content derived from the mutated `X`.
- **compile-error / stale (self-healing)** — the build fails, in generated or user code; the
  developer is forced to touch something, after which regeneration heals the state.
- **silent-stale-wrong** — the build succeeds and `E_` still carries pre-mutation metadata.
  The unacceptable outcome: wrong data with no signal.

Verdict rule: **any** silent-stale-wrong under isolating ⇒ "isolating unacceptable alone —
aggregating or hybrid required"; otherwise ⇒ "isolating acceptable".

## The retention hypothesis (registered up front)

The outcome under isolating is predicted to hinge on `@SpikeReferences`' retention, not on the
processor category alone. With **CLASS** retention, `X.class` sits in `E`'s constant pool as an
annotation value, so Gradle's incremental-compilation dependency analysis sees `E → X` and
recompiles `E` whenever `X` changes — reprocessing `E` and regenerating `E_` *correctly* even
under isolating. With **SOURCE** retention that bytecode edge disappears, `E` is not recompiled,
and the isolating processor is never re-invoked over `E` — predicted silent-stale-wrong for
mutations (a) and (b).

## Predictions

| Mutation | isolating, CLASS retention | isolating, SOURCE retention | aggregating |
|---|---|---|---|
| (a) rename `@Id` property | regenerated-correct (dependency edge recompiles `E`) | **silent-stale-wrong** | regenerated-correct |
| (b) `@Id` type `Long`→`String` | regenerated-correct (same edge) | **silent-stale-wrong** | regenerated-correct |
| (c) delete `X.java` | compile-error in **user code** (`E.java` references `X.class`) | compile-error in user code (source reference still present) | compile-error in user code |

Control (validity guard, isolating only): after a full build, an incremental rebuild with only a
comment change to `E.java` must reprocess `E` alone with incremental annotation processing
engaged — no "not incremental" warning, no full recompilation.

Both variants pass **exactly one** originating element (`E` for `E_`); the isolating rule demands
it, and keeping the variants identical apart from the registration file isolates the category as
the only experimental variable.
