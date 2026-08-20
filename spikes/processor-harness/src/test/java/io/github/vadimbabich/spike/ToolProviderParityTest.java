package io.github.vadimbabich.spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Charter question 1: can a JSR-269 processor reproduce a committed golden file byte-for-byte?
 *
 * <p>{@code ToolProvider} answers it first because it ships with the JDK. The sugar harnesses in
 * round 2 only re-assert a parity that is already green here, so a red cell there is a harness
 * finding and can never be mistaken for a processor defect.
 */
class ToolProviderParityTest {

  @TempDir
  Path workDir;

  private GoldenCorpus corpus;
  private CompilationOutcome outcome;

  @BeforeEach
  void compileGoldenEntities() {
    corpus = GoldenCorpus.locate();

    List<Path> sources = new ArrayList<>(corpus.entitySources());
    sources.addAll(corpus.supportSources());

    outcome = new SpikeCompiler(workDir).compile(sources, new SpikeMetamodelProcessor());
  }

  @Test
  void compilesTheGoldenEntitiesWithoutErrors() {
    assertThat(outcome.errors()).isEmpty();
    assertThat(outcome.succeeded()).isTrue();
  }

  @Test
  void reproducesTheGoldenCorpusByteForByte() {
    MetamodelParity.against(corpus).assertMatches(outcome.generatedSources());
  }
}
