# Runbook — Golden-Corpus Update

## Status

The golden corpus is `src/it/simple-consumer/expected/` — **4 expected `.java` files**
byte-compared by the invoker IT during `mvn verify`. It currently freezes the **1.x
shape**, including the two infrastructure files the generator emits into Spring's own
packages (`Column_`, `StaticR2dbcEntityTemplateAccessor_`). That shape is a regression
tripwire, **not an endorsement** — the v2 design (D1) replaces it, and the split-package
emission disappears with it.

Nothing about the v2 shape is ratified yet: D1 is gated, and the member-ordering and
`@Generated` decisions are Proposed ADRs in `_doc/adr/`. Until they are ratified, the
corpus records what the generator emits today; do not "pre-apply" a proposed shape.

## Rules

1. **No corpus change without an approving ADR.** The ADR must exist and be dated before
   the change is staged, and the **commit message must name it**. The ADRs live in
   `_doc/adr/` and are not committed (private working material), so the written-decision
   discipline is enforced by the commit message rather than by a same-commit diff.
2. Generated output must remain byte-deterministic: no dates, no absolute paths, no
   environment-dependent content, and a stable member order
   (proposed rule: `_doc/adr/adr-inheritance-representation-and-member-ordering.md`).
3. Every corpus file must compile before ratification — the IT compiles the generated
   output against real Spring Data artifacts. Never accept hand-authored expected sources
   on eyeball review; generics-heavy files in particular must be machine-verified.

## Procedure

```bash
# 1. Make the generator change on a feature branch (feature/YYYY.MM_desc).
# 2. Run the IT and inspect the byte-diff it reports:
mvn verify
# 3. If the diff is exactly the intended shape change, update the expected sources,
#    then prove determinism by running twice — both must be green and identical:
mvn verify && mvn verify
# 4. Stage explicitly (never git add .):
git add src/it/simple-consumer/expected/...
# 5. Commit message names the approving ADR by filename; the ADR itself stays in _doc/.
```

## Red flags — stop and escalate

- A corpus diff you did not intend → determinism regression; find the source of
  non-determinism before touching expected files.
- A diff that appears only on some JDKs or only in an IDE build → compiler-dependent
  element ordering. This is a known hazard (javac vs ECJ, and binary-sourced types); it
  is the reason a generator-imposed total order is proposed rather than relying on
  declaration order.
- Any temptation to update expected files "to make the build green" without an ADR — that
  converts a contract change into an accident, which is exactly what this gate prevents.
