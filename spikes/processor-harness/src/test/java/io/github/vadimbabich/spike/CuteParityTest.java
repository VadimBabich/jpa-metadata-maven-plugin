package io.github.vadimbabich.spike;

import io.toolisticon.cute.Cute;
import io.toolisticon.cute.CuteApi;
import io.toolisticon.cute.JavaFileObjectUtils;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Round 2, harness 2: the identical parity assertion expressed through Cute 1.9.0.
 *
 * <p>Cute is the only candidate with a first-class byte-level matcher
 * ({@code ExpectedFileObjectMatcherKind.BINARY}), so the assertion needs no hand-written byte
 * comparison. The cost is shape: compilation and assertion are one fluent chain that cannot be
 * split, so each check re-compiles.
 */
class CuteParityTest {

  private GoldenCorpus corpus;

  @BeforeEach
  void locateCorpus() {
    corpus = GoldenCorpus.locate();
  }

  @Test
  void compilesTheGoldenEntitiesWithoutErrors() {
    givenTheGoldenEntities()
        .whenCompiled()
        .thenExpectThat()
        .compilationSucceeds()
        .executeTest();
  }

  @Test
  void reproducesTheGoldenCorpusByteForByte() {
    CuteApi.CompilerTestExpectAndThatInterface expectation = givenTheGoldenEntities()
        .whenCompiled()
        .thenExpectThat()
        .compilationSucceeds();

    // The chain cannot be built by a stream: every step returns a different interface, so the
    // builder is reassigned per golden file. Recorded as an ergonomics observation, not a defect.
    for (Map.Entry<String, Path> golden : corpus.goldenMetamodels().entrySet()) {
      expectation = expectation.andThat()
          .generatedSourceFile(qualifiedNameOf(golden.getKey()))
          .matches(CuteApi.ExpectedFileObjectMatcherKind.BINARY, asJavaFileObject(golden.getValue()));
    }

    expectation.executeTest();
  }

  private CuteApi.BlackBoxTestFinalGivenInterface givenTheGoldenEntities() {
    List<Path> sources = new ArrayList<>(corpus.entitySources());
    sources.addAll(corpus.supportSources());

    List<JavaFileObject> sourceObjects = new ArrayList<>();
    for (Path source : sources) {
      sourceObjects.add(asJavaFileObject(source));
    }

    return Cute.blackBoxTest()
        .given()
        .processor(SpikeMetamodelProcessor.class)
        .andSourceFiles(sourceObjects.toArray(new JavaFileObject[0]))
        .andUseCompilerOptions("--release 17", "-encoding UTF-8");
  }

  private JavaFileObject asJavaFileObject(Path source) {
    try {
      return JavaFileObjectUtils.readFromUrl(source.toUri().toURL());
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Cannot address corpus file " + source, e);
    }
  }

  private String qualifiedNameOf(String relativePath) {
    return relativePath
        .replace(".java", "")
        .replace('/', '.');
  }
}
