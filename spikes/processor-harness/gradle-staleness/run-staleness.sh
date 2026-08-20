#!/usr/bin/env bash
# G3 staleness experiment runner. Executes the pre-registered scenario from expected-outcomes.md:
# per registration category, a control run plus three mutations of X.java, each from a pristine
# fixture state, with the incremental rebuild never preceded by a clean.
#
# Environment:
#   GRADLE_BIN  path to a gradle launcher (default: gradle on PATH)
#   JAVA_HOME   JDK for Gradle (default: JDK 21 via /usr/libexec/java_home)
set -uo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
GRADLE_BIN="${GRADLE_BIN:-gradle}"
GRADLE_ARGS=(--console=plain --info)

GENERATED="build/generated/sources/annotation_processor/java/main/fixture/E_.java"
# Gradle's generated-sources directory name differs across versions; resolve at run time.
resolve_generated() {
  find build/generated -name 'E_.java' 2>/dev/null | head -1
}

RESULTS_DIR="results"
mkdir -p "${RESULTS_DIR}"

restore_pristine() {
  rm -rf src
  mkdir -p src/main/java/fixture
  cp pristine/E.java pristine/X.java src/main/java/fixture/
}

full_build() {
  local category="$1" log="$2"
  "${GRADLE_BIN}" "${GRADLE_ARGS[@]}" -PprocessorJar="variants/processor-${category}.jar" \
    clean compileJava >"${log}" 2>&1
}

incremental_build() {
  local category="$1" log="$2"
  "${GRADLE_BIN}" "${GRADLE_ARGS[@]}" -PprocessorJar="variants/processor-${category}.jar" \
    compileJava >"${log}" 2>&1
}

record() {
  local label="$1" exit_code="$2" e_before="$3" log="$4"
  local e_file e_after regenerated stale_content

  e_file=$(resolve_generated || true)
  if [[ -n "${e_file}" && -f "${e_file}" ]]; then
    e_after=$(shasum -a 256 "${e_file}" | cut -d' ' -f1)
  else
    e_after="ABSENT"
  fi

  if [[ "${e_after}" == "${e_before}" ]]; then regenerated="no"; else regenerated="yes"; fi

  stale_content="n/a"
  if [[ -n "${e_file}" && -f "${e_file}" ]]; then
    if grep -q 'REF_TARGET_ID_PROPERTY = "id"' "${e_file}"; then
      stale_content='property="id"'
    elif grep -q 'REF_TARGET_ID_PROPERTY = "identifier"' "${e_file}"; then
      stale_content='property="identifier"'
    fi
    if grep -q 'REF_TARGET_ID_TYPE = "java.lang.String"' "${e_file}"; then
      stale_content="${stale_content} type=String"
    elif grep -q 'REF_TARGET_ID_TYPE = "java.lang.Long"' "${e_file}"; then
      stale_content="${stale_content} type=Long"
    fi
  fi

  local incremental_note=""
  if grep -q "not incremental" "${log}"; then
    incremental_note=" [processor flagged NOT incremental]"
  fi
  if grep -q "Full recompilation is required" "${log}"; then
    incremental_note="${incremental_note} [full recompilation]"
  fi

  echo "${label} | exit=${exit_code} | E_ regenerated=${regenerated} | E_ content: ${stale_content}${incremental_note}"
}

run_category() {
  local category="$1"
  local out="${RESULTS_DIR}/${category}.txt"
  : >"${out}"

  echo "### category=${category} · gradle=$("${GRADLE_BIN}" --version 2>/dev/null | awk '/^Gradle/{print $2}') · retention=$(unzip -p "variants/processor-${category}.jar" io/github/vadimbabich/spike/SpikeReferences.class | strings | grep -q RetentionPolicy && echo 'see-jar')" >>"${out}"

  # Control (validity guard): comment-only change to E must reprocess E incrementally.
  restore_pristine
  full_build "${category}" "${RESULTS_DIR}/${category}-control-full.log"
  local e0; e0=$(shasum -a 256 "$(resolve_generated)" | cut -d' ' -f1)
  echo "// control touch" >> src/main/java/fixture/E.java
  incremental_build "${category}" "${RESULTS_DIR}/${category}-control-incr.log"; local rc=$?
  record "control(touch E)" "${rc}" "${e0}" "${RESULTS_DIR}/${category}-control-incr.log" >>"${out}"
  if grep -q "Incremental compilation of" "${RESULTS_DIR}/${category}-control-incr.log"; then
    echo "control: incremental compilation ENGAGED" >>"${out}"
  else
    echo "control: incremental compilation NOT CONFIRMED — check log" >>"${out}"
  fi

  # Mutation (a): rename X's @Id property.
  restore_pristine
  full_build "${category}" "${RESULTS_DIR}/${category}-a-full.log"
  local ea; ea=$(shasum -a 256 "$(resolve_generated)" | cut -d' ' -f1)
  sed -i '' 's/Long id;/Long identifier;/' src/main/java/fixture/X.java
  incremental_build "${category}" "${RESULTS_DIR}/${category}-a-incr.log"; rc=$?
  record "(a) rename @Id property" "${rc}" "${ea}" "${RESULTS_DIR}/${category}-a-incr.log" >>"${out}"

  # Mutation (b): change the @Id type Long -> String.
  restore_pristine
  full_build "${category}" "${RESULTS_DIR}/${category}-b-full.log"
  local eb; eb=$(shasum -a 256 "$(resolve_generated)" | cut -d' ' -f1)
  sed -i '' 's/Long id;/String id;/' src/main/java/fixture/X.java
  incremental_build "${category}" "${RESULTS_DIR}/${category}-b-incr.log"; rc=$?
  record "(b) @Id type Long->String" "${rc}" "${eb}" "${RESULTS_DIR}/${category}-b-incr.log" >>"${out}"

  # Mutation (c): delete X.java.
  restore_pristine
  full_build "${category}" "${RESULTS_DIR}/${category}-c-full.log"
  local ec; ec=$(shasum -a 256 "$(resolve_generated)" | cut -d' ' -f1)
  rm src/main/java/fixture/X.java
  incremental_build "${category}" "${RESULTS_DIR}/${category}-c-incr.log"; rc=$?
  record "(c) delete X.java" "${rc}" "${ec}" "${RESULTS_DIR}/${category}-c-incr.log" >>"${out}"
  grep -E "error:" "${RESULTS_DIR}/${category}-c-incr.log" | head -5 >>"${out}" || true

  echo "--- ${category} done; results in ${out}"
  cat "${out}"
}

run_category isolating
run_category aggregating
