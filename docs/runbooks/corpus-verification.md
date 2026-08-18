# Runbook — Evidence-Corpus Verification (G0)

## Status

Gate **G0 is closed** (attested and signed 2026-08-16; extended by a live-drift addendum
2026-08-18). Corpus-derived quantities graduate from `[review-time]` to *attested* only by
citing the exit file — never by restating a number. Closure mechanism: attestation-with-
manifest, not publication of the corpus (`_doc/adr/adr-corpus-restore-vs-reattest.md`).

The corpus is another party's proprietary source, held locally only. It is gitignored and
must never be committed. **Read its current scope and figures from the manifest of record
rather than from this runbook** — the corpus has grown once already, and hard-coded counts
here would rot:

- manifest of record: the newest `_doc/corpus-manifest-*.sha256` (older dated manifests are
  kept as historical records and are never overwritten);
- exit file (verdicts, methods, addenda): `_doc/investigations/attemp_1/02-corpus-reattestation.md`.

The manifests are themselves private: they list proprietary file paths, so a redacted
digest-only variant is required before any attestation is published.

This runbook needs local corpus access; without it, only step 1 applies.

## 1. Guard check (safe to run anywhere)

```bash
git check-ignore _doc/repo_usage _doc/repository && echo GUARDED || echo "DANGER: guard missing"
git ls-files _doc | head              # MUST print nothing — _doc is fully gitignored
```

## 2. Manifest verification (BagIt discipline: valid *and* complete)

```bash
M=$(ls -1 _doc/corpus-manifest-*.sha256 | tail -1)   # manifest of record = newest
# valid — every recorded hash still matches its file:
grep -v '^#' "$M" | shasum -a 256 -c | grep -c OK
# complete — no payload file missing from the manifest
# (payload excludes IDE state by definition; see the manifest header):
diff <(find _doc/repo_usage _doc/repository -type f -not -path '*/.idea/*' | sort) \
     <(grep -v '^#' "$M" | awk '{print $2}' | sort)
```

Both must be clean. If either fails the corpus has changed: **do not edit the existing
manifest** — write a new dated one, re-run step 3, and record which claims moved in a
dated addendum to the exit file (never rewrite signed content).

## 3. Re-measure the attested quantities

Expected values live in the exit file's verdict table (and its addenda), not here — read
them from there and compare. The measurements:

```bash
cd _doc/repo_usage
# property-name call sites (subtract helper definitions to get real call sites):
grep -rn "nameOf(" --include=*.java . | wc -l
grep -rn -E "String nameOf\(" --include=*.java . | wc -l
# join inventory:
grep -rn '\.join(' --include=*.java . | wc -l
grep -rn 'leftOuterJoin(' --include=*.java . | wc -l
# closed-algebra boundary (expect zero in query-DSL code):
grep -rn -iE "GROUP BY|HAVING| UNION | EXISTS \(" --include=*.java . | grep -viE '@Query|^\s*\*' | wc -l
# composite keys (expect zero; beware domain identifiers containing "IDClass"):
grep -rn -iE "IdClass|EmbeddedId" --include=*.java . | wc -l
# entity/type count and payload size:
grep -rl "core.mapping.Table" --include=*.java */src/main/java 2>/dev/null | wc -l
cd ../.. && find _doc/repo_usage _doc/repository -type f -not -path '*/.idea/*' | wc -l
```

**Field-annotation coverage (the ADR-004 input) needs a parser, not grep** — annotation
blocks span many lines and Builder/test classes produce false positives. Use JavaParser
with the language level set to the entities' Java version (the disposable spike used for
the 2026-08-18 addendum is documented in the exit file; re-create it rather than trusting
a regex).

Why these numbers matter: the call-site count is the migration-effort figure; the join
inventory sizes the relationship-classification work; the **zero** for GROUP BY/HAVING/
UNION/subselects is the evidence for the closed query algebra (scope boundary); the type
count calibrates processor-throughput budgets. A mismatch means a claim moved — fix the
claim, not the number.

## 4. Known limits

- **Scope grows.** The corpus was repository-layer only at first attestation and later
  became three complete services; measured *absences* are only as broad as the current
  payload. State the payload with any absence claim.
- **Duplicates.** One historical extract is a byte-identical copy of a package inside a
  later-added service — exclude it from distinct counts (the manifest header flags it).
- **Excluded by definition.** IDE state (`.idea/**`) is not payload: no evidentiary value,
  non-trivial sensitivity (data-source metadata). Keep it out of measurements and archives.
