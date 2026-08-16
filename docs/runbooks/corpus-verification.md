# Runbook — Evidence-Corpus Verification (G0)

## Status

Gate **G0 is open**. Quantities derived from the evidence corpus are marked
`[review-time]` in the HLD and the ADRs and must not be restated as verified facts.
G0's proposed closure mechanism is attestation-with-manifest, not publication of the
corpus (`_doc/adr/adr-corpus-restore-vs-reattest.md`, Proposed — not ratified).

The corpus is another party's proprietary source: **32 files, 4,521 lines**, at
`_doc/repo_usage/` (28 files, production repository implementations) and
`_doc/repository/` (4 files, the in-house `ReactiveEntityManager`/`QueryBuilder`
infrastructure). It is gitignored and must never be committed. Its manifest,
`_doc/corpus-manifest-2026-08-16.sha256`, is also private — it lists proprietary file
paths (production class names), so a redacted digest-only variant is required before
any attestation is published.

This runbook needs local corpus access; without it, only step 1 applies.

## 1. Guard check (safe to run anywhere)

```bash
git check-ignore _doc/repo_usage _doc/repository && echo GUARDED || echo "DANGER: guard missing"
git ls-files _doc | head              # MUST print nothing — _doc is fully gitignored
```

## 2. Manifest verification (BagIt discipline: valid *and* complete)

```bash
# valid — every recorded hash still matches its file:
grep -v '^#' _doc/corpus-manifest-2026-08-16.sha256 | shasum -a 256 -c   # expect 32× OK
# complete — no corpus file missing from the manifest:
diff <(find _doc/repo_usage _doc/repository -type f | sort) \
     <(grep -v '^#' _doc/corpus-manifest-2026-08-16.sha256 | awk '{print $2}' | sort)
```

Both must be clean. If either fails the corpus has changed: **do not edit the existing
manifest** — write a new dated one, re-run step 3, and record which claims moved.

## 3. Re-measure the attested quantities

```bash
cd _doc/repo_usage
grep -rn "nameOf(" --include=*.java . | wc -l                     # 92 raw hits
grep -rn -E "String nameOf\(" --include=*.java . | wc -l          # 7 helper definitions
                                                                  # → 85 real call sites
grep -rhoE "@[A-Z][A-Za-z]+" --include=*.java . | sort | uniq -c  # @Override 81, @Query 3,
                                                                  # @FunctionalInterface 1
grep -rn -iE "GROUP BY|HAVING|UNION|SELECT.*\(SELECT" --include=*.java . | wc -l   # 0
cd ../..
find _doc/repo_usage _doc/repository -type f | wc -l              # 32
find _doc/repo_usage _doc/repository -type f | sort | xargs cat | wc -l   # 4521
```

Why these numbers matter: **85/7** corrects the widely-quoted "92 `nameOf` sites" (a raw
grep count) and is the migration-effort figure; the **annotation inventory** is the
measured basis for the name-constants decision; the **zero** for GROUP BY/HAVING/UNION/
subselects is the evidence for the closed query algebra (scope boundary). A mismatch
means a claim moved — fix the claim, not the number.

## 4. Known limit of this measurement

The corpus is repository-layer only — no entity classes, no controllers, no web layer.
Absences measured here (notably "no annotation-value property names") are therefore
partly an artifact of the slice, not proof about the whole production codebase. When
access to the full production codebase is available, re-run step 3 across it; that sweep
is also the demand-trigger check for the name-constants decision.
