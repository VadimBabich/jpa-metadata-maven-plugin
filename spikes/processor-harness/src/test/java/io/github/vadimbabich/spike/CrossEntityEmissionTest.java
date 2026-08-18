package io.github.vadimbabich.spike;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Cross-entity mode, happy path: {@code @SpikeReferences(target = X.class)} must emit a
 * self-contained stand-in recording X's {@code @Id} property — the read-through-the-annotation
 * that rounds 3 and 4 probe. The stand-in shape replaces the 1.x parity shape for such entities;
 * it deliberately needs no golden support classes, so the Gradle fixture stays minimal.
 */
class CrossEntityEmissionTest {

  @TempDir
  Path workDir;

  @Test
  void emitsAStandInRecordingTheTargetsIdPropertyNameAndType() {
    CompilationOutcome outcome = new SpikeCompiler(workDir).compile(
        List.of(
            BrokenSourceFixtures.fixture("ref/RefTarget.java"),
            BrokenSourceFixtures.fixture("ref/RefEntity.java")),
        new SpikeMetamodelProcessor());

    assertThat(outcome.errors()).isEmpty();
    assertThat(outcome.succeeded()).isTrue();

    byte[] emitted = outcome.generatedSources().get("ref/RefEntity_.java");
    assertThat(emitted).as("ref/RefEntity_.java was not emitted").isNotNull();

    String emittedSource = new String(emitted, UTF_8);
    assertThat(emittedSource)
        .contains("public static final Class<?> REF_TARGET = ref.RefTarget.class;")
        .contains("public static final String REF_TARGET_ID_PROPERTY = \"id\";")
        .contains("public static final String REF_TARGET_ID_TYPE = \"java.lang.Long\";");

    // The referenced side is a class, not a record: no metamodel for it, and no 1.x-shape file
    // for the referencing side either — the stand-in replaces it.
    assertThat(outcome.generatedSources()).containsOnlyKeys("ref/RefEntity_.java");
  }
}
