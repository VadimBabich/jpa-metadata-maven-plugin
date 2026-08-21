#!/usr/bin/env bash
#
# Prints the narrowest git range the email guard should scan. Manual dispatch and force-pushes
# leave no usable base commit, and the guard refuses an empty range, so the last candidate must
# always yield one.
#
# Environment (both optional): PULL_REQUEST_BASE_SHA, PUSH_BEFORE_SHA.

set -euo pipefail

readonly NULL_SHA='0000000000000000000000000000000000000000'
readonly DEFAULT_BASE_BRANCH='origin/master'

log_warn() {
  printf '[%s] WARN  %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$1" >&2
}

log_error() {
  printf '[%s] ERROR %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$1" >&2
}

trap 'log_error "unexpected failure at line ${LINENO}"; exit 1' ERR

commit_exists() {
  local revision="$1"

  git cat-file -e "${revision}^{commit}" 2>/dev/null
}

pull_request_range() {
  local base_sha="${PULL_REQUEST_BASE_SHA:-}"

  if [[ -z "${base_sha}" ]]; then
    return 0
  fi

  if ! commit_exists "${base_sha}"; then
    return 0
  fi

  printf '%s..HEAD\n' "${base_sha}"
}

push_range() {
  local before_sha="${PUSH_BEFORE_SHA:-}"

  if [[ -z "${before_sha}" || "${before_sha}" == "${NULL_SHA}" ]]; then
    return 0
  fi

  if ! commit_exists "${before_sha}"; then
    return 0
  fi

  printf '%s..HEAD\n' "${before_sha}"
}

merge_base_range() {
  local merge_base

  if ! commit_exists "${DEFAULT_BASE_BRANCH}"; then
    return 0
  fi

  merge_base="$(git merge-base "${DEFAULT_BASE_BRANCH}" HEAD)"

  if [[ -z "${merge_base}" ]]; then
    return 0
  fi

  if [[ "${merge_base}" == "$(git rev-parse HEAD)" ]]; then
    return 0
  fi

  printf '%s..HEAD\n' "${merge_base}"
}

tip_commit_range() {
  if ! commit_exists 'HEAD~1'; then
    return 0
  fi

  log_warn "no base commit available: scanning only the tip commit, earlier commits in this push are not examined"

  printf 'HEAD~1..HEAD\n'
}

main() {
  local candidate
  local range

  for candidate in pull_request_range push_range merge_base_range tip_commit_range; do
    range="$("${candidate}")"

    if [[ -n "${range}" ]]; then
      printf '%s\n' "${range}"
      return 0
    fi
  done

  log_error "could not resolve a scan range: HEAD has no parent and no base is reachable"
  return 1
}

if ! main "$@"; then
  exit 1
fi
