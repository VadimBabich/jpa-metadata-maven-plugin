package io.github.vadimbabich.spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

/**
 * The parity assertion itself, kept apart from the compiler that produced the bytes so every
 * harness in round 2 asserts the identical thing.
 *
 * <p>Bytes, never an AST: the shape freeze is a promise about a file's contents, and an
 * AST-equivalence assertion would stay green while the header, encoding or blank lines drifted.
 */
final class MetamodelParity {

  private final Map<String, byte[]> goldenSources;

  private MetamodelParity(Map<String, byte[]> goldenSources) {
    this.goldenSources = goldenSources;
  }

  static MetamodelParity against(GoldenCorpus corpus) {
    Map<String, byte[]> goldenSources = new TreeMap<>();

    corpus.goldenMetamodels().forEach((relativePath, file) -> {
      try {
        goldenSources.put(relativePath, Files.readAllBytes(file));
      } catch (IOException e) {
        throw new UncheckedIOException("Cannot read golden file " + file, e);
      }
    });

    return new MetamodelParity(goldenSources);
  }

  void assertMatches(Map<String, byte[]> generatedSources) {
    assertThat(generatedSources.keySet())
        .as("emitted file set")
        .containsExactlyInAnyOrderElementsOf(goldenSources.keySet());

    for (Map.Entry<String, byte[]> golden : goldenSources.entrySet()) {
      assertThat(generatedSources.get(golden.getKey()))
          .as("%s differs from the golden corpus", golden.getKey())
          .isEqualTo(golden.getValue());
    }
  }
}
