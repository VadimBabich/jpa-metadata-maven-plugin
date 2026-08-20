package io.github.vadimbabich.spike;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Charter question 2, under {@code ToolProvider} — the authoritative behavior record. What does a
 * processor observe when compilation is already failing (the normal case inside an IDE), and can
 * it still validate and report?
 *
 * <p>BS-1/BS-2 are experiments: their outcome classification was registered in the plan before
 * execution, and the assertions below document observed javac behavior as a regression record.
 * BS-3 is a behavioral requirement (processor-side validation) and was test-driven.
 */
class BrokenSourceProbeTest {

  @TempDir
  Path workDir;

  @Nested
  class Bs1UnresolvableComponentType {

    @Test
    void processorIsStillInvokedAndSeesTheComponentAsErrorType() {
      ObservingProcessor processor = new ObservingProcessor();

      CompilationOutcome outcome = new SpikeCompiler(workDir).compile(
          withGoldenSupportSources(BrokenSourceFixtures.fixture("bs1/Bs1Entity.java")),
          processor);

      assertThat(outcome.succeeded()).isFalse();
      assertThat(outcome.errors()).isNotEmpty();

      // The processor runs over the broken entity anyway — an IDE-invoked processor must expect
      // exactly this input.
      ObservingProcessor.RoundObservation firstRound = processor.rounds().get(0);
      assertThat(firstRound.annotatedRecords()).containsExactly("bs1.Bs1Entity");

      // The unresolvable component arrives as an ERROR TypeMirror; the resolvable one is intact.
      assertThat(firstRound.componentTypeKinds())
          .containsEntry("Bs1Entity.missing", "ERROR")
          .containsEntry("Bs1Entity.name", "DECLARED");
    }

    @Test
    void emissionStillHappensBecauseTheParityShapeNeverReadsComponentTypes() {
      CompilationOutcome outcome = new SpikeCompiler(workDir).compile(
          withGoldenSupportSources(BrokenSourceFixtures.fixture("bs1/Bs1Entity.java")),
          new SpikeMetamodelProcessor());

      assertThat(outcome.generatedSources()).containsKey("bs1/Bs1Entity_.java");
    }
  }

  @Nested
  class Bs2ErrorInAnUnrelatedUnit {

    @Test
    void processorStillRunsOverTheValidEntityAndItsOutputIsProduced() {
      ObservingProcessor processor = new ObservingProcessor();

      List<Path> sources = withGoldenSupportSources(
          BrokenSourceFixtures.fixture("bs2/BrokenUnit.java"));
      sources.addAll(GoldenCorpus.locate().entitySources());

      CompilationOutcome outcome = new SpikeCompiler(workDir).compile(sources, processor);

      assertThat(outcome.succeeded()).isFalse();

      ObservingProcessor.RoundObservation firstRound = processor.rounds().get(0);
      assertThat(firstRound.annotatedRecords())
          .containsExactlyInAnyOrder("com.example.model.User", "com.example.model.UserAttribute");

      // The valid entities' metamodels are written despite the failing unit.
      assertThat(outcome.generatedSources())
          .containsKeys("com/example/model/User_.java", "com/example/model/UserAttribute_.java");
    }

    @Test
    void errorRaisedNeverFiresForABodyLevelSemanticError() {
      ObservingProcessor processor = new ObservingProcessor();

      List<Path> sources = withGoldenSupportSources(
          BrokenSourceFixtures.fixture("bs2/BrokenUnit.java"));
      sources.addAll(GoldenCorpus.locate().entitySources());

      new SpikeCompiler(workDir).compile(sources, processor);

      List<ObservingProcessor.RoundObservation> rounds = processor.rounds();
      ObservingProcessor.RoundObservation finalRound = rounds.get(rounds.size() - 1);

      assertThat(finalRound.processingOver()).isTrue();

      // Observed, initially against the prediction: the method-body type error is only detected
      // at attribution, after every processing round has completed — so errorRaised() is false
      // in all rounds, including the last. A processor cannot use errorRaised() to learn that
      // the compilation it is part of is going to fail.
      assertThat(finalRound.errorRaised()).isFalse();
    }
  }

  @Nested
  class Bs3ReferenceValidation {

    @Test
    void unresolvableTargetProducesTheProcessorsOwnDiagnostic() {
      CompilationOutcome outcome = new SpikeCompiler(workDir).compile(
          List.of(BrokenSourceFixtures.fixture("bs3/MissingTargetEntity.java")),
          new SpikeMetamodelProcessor());

      assertThat(outcome.succeeded()).isFalse();

      // javac reports the unresolvable symbol on its own; the requirement is the processor's
      // additional diagnostic, proving it observed the annotation value as an ERROR type.
      assertThat(outcome.errors())
          .anySatisfy(error -> assertThat(error).contains("does not resolve"));
    }

    @Test
    void targetWithoutIdPropertyFailsCompilationWithTheValidationMessage() {
      CompilationOutcome outcome = new SpikeCompiler(workDir).compile(
          List.of(
              BrokenSourceFixtures.fixture("bs3/NoIdTarget.java"),
              BrokenSourceFixtures.fixture("bs3/NoIdEntity.java")),
          new SpikeMetamodelProcessor());

      assertThat(outcome.succeeded()).isFalse();
      assertThat(outcome.errors())
          .anySatisfy(error -> assertThat(error).contains("has no @Id property"));

      // The diagnostic must be anchored on the referencing entity, where the fix belongs.
      assertThat(outcome.errors())
          .anySatisfy(error -> assertThat(error).contains("NoIdEntity"));

      assertThat(outcome.generatedSources()).doesNotContainKey("bs3/NoIdEntity_.java");
    }
  }

  private List<Path> withGoldenSupportSources(Path fixture) {
    List<Path> sources = new ArrayList<>();
    sources.add(fixture);
    sources.addAll(GoldenCorpus.locate().supportSources());

    return sources;
  }
}
