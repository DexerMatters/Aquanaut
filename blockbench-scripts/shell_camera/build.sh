#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TPL="${BLOCKBENCH_SKILL_TEMPLATES:-/home/dexer/.pi/agent/npm/node_modules/@dexer_matters/pi-blockbench/skills/blockbench-script/templates}"
assemble() {
  local body="$1"
  local out="$2"
  {
    printf '(() => {\n'
    grep -v '^[[:space:]]*//' "$TPL/runtime.js"
    printf '\n'
    cat "$body"
    printf '})();\n'
  } | grep -v '^[[:space:]]*//' > "$out"
  if grep -q '//' "$out"; then
    echo "comment survived in $out" >&2
    exit 1
  fi
  if grep -q 'console\.' "$out"; then
    echo "console call survived in $out" >&2
    exit 1
  fi
}
for pair in model texture; do
  assemble "$HERE/src/$pair.body.js" "$HERE/$pair.js"
done
wc -c "$HERE/model.js" "$HERE/texture.js"
