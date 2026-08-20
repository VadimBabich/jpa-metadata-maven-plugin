package io.github.vadimbabich.spike;

import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.toolisticon.cute.Cute;
import io.toolisticon.cute.JavaFileObjectUtils;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

/**
 * Phase-2 criterion closed by Phase 3: can each sugar harness <em>express</em> the broken-source
 * assertions — failed compilation plus a specific processor diagnostic? The behavior itself is
 * already recorded authoritatively under {@code ToolProvider} in {@link BrokenSourceProbeTest};
 * these tests only measure assertion ergonomics.
 */
class BrokenSourceHarnessExpressibilityTest {

  @Test
  void compileTestingExpressesTheBs3FailureAndDiagnostic() {
    Compilation compilation = javac()
        .withProcessors(new SpikeMetamodelProcessor())
        .withOptions("--release", "17", "-encoding", "UTF-8",
            "-classpath", System.getProperty("java.class.path"))
        .compile(
            forResource(BrokenSourceFixtures.fixture("bs3/NoIdTarget.java")),
            forResource(BrokenSourceFixtures.fixture("bs3/NoIdEntity.java")));

    assertThat(compilation.status()).isEqualTo(Compilation.Status.FAILURE);
    assertThat(compilation.errors())
        .anySatisfy(error ->
            assertThat(error.getMessage(null)).contains("has no @Id property"));

    // Expressibility limit, recorded: generatedSourceFile() throws IllegalStateException once
    // compilation has failed ("generated files are unavailable"), so "nothing was emitted for
    // the rejected entity" cannot be asserted through compile-testing at all. ToolProvider and
    // Cute can both express it.
    assertThat(catchIllegalState(() -> compilation.generatedSourceFile("bs3.NoIdEntity_")))
        .hasMessageContaining("generated files are unavailable");
  }

  private IllegalStateException catchIllegalState(Runnable inspection) {
    try {
      inspection.run();
    } catch (IllegalStateException expected) {
      return expected;
    }

    throw new AssertionError(
        "compile-testing now allows inspecting generated files after a failed compilation — "
            + "update the recorded expressibility limit");
  }

  @Test
  void cuteExpressesTheBs3FailureAndDiagnostic() {
    Cute.blackBoxTest()
        .given()
        .processor(SpikeMetamodelProcessor.class)
        .andSourceFiles(
            readFromUrl(BrokenSourceFixtures.fixture("bs3/NoIdTarget.java")),
            readFromUrl(BrokenSourceFixtures.fixture("bs3/NoIdEntity.java")))
        .andUseCompilerOptions("--release 17", "-encoding UTF-8")
        .whenCompiled()
        .thenExpectThat()
        .compilationFails()
        .andThat()
        .compilerMessage()
        .ofKindError()
        .contains("has no @Id property")
        .executeTest();
  }

  private JavaFileObject forResource(Path fixture) {
    try {
      return JavaFileObjects.forResource(fixture.toUri().toURL());
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Cannot address fixture " + fixture, e);
    }
  }

  private JavaFileObject readFromUrl(Path fixture) {
    try {
      return JavaFileObjectUtils.readFromUrl(fixture.toUri().toURL());
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Cannot address fixture " + fixture, e);
    }
  }
}
