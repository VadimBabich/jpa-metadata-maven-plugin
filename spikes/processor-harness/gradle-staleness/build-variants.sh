#!/usr/bin/env bash
# Builds the two processor-jar variants that differ only in
# META-INF/gradle/incremental.annotation.processors.
set -euo pipefail
cd "$(dirname "$0")"

mvn -B -q -f ../pom.xml clean package -DskipTests

mkdir -p variants
for category in isolating aggregating; do
  cp ../target/processor-harness-spike-0-SPIKE.jar "variants/processor-${category}.jar"

  staging=$(mktemp -d)
  mkdir -p "${staging}/META-INF/gradle"
  echo "io.github.vadimbabich.spike.SpikeMetamodelProcessor,${category}" \
    > "${staging}/META-INF/gradle/incremental.annotation.processors"

  (cd "${staging}" && jar -uf "$(cd - >/dev/null; pwd)/variants/processor-${category}.jar" \
    META-INF/gradle/incremental.annotation.processors)
  rm -rf "${staging}"
done

echo "variants built:"
for category in isolating aggregating; do
  unzip -p "variants/processor-${category}.jar" META-INF/gradle/incremental.annotation.processors
done
