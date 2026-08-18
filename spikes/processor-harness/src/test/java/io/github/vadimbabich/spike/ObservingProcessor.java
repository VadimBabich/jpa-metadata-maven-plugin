package io.github.vadimbabich.spike;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * The spike processor plus a per-round observation log — the instrument for BS-1 and BS-2, where
 * the question is not "does it pass" but "what does a processor see when compilation is already
 * failing".
 *
 * <p>Observation lives in the test tree so the processor under test stays exactly the artifact
 * the other rounds measure.
 *
 * <p>The annotations are re-declared deliberately: {@code AbstractProcessor} reads them from
 * {@code this.getClass()}, so a subclass without them advertises an <em>empty</em> supported set
 * and is silently never invoked. A finding in its own right — any test double subclassing a
 * processor must re-declare both.
 */
@SupportedAnnotationTypes("org.springframework.data.relational.core.mapping.Table")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
final class ObservingProcessor extends SpikeMetamodelProcessor {

  /**
   * @param componentTypeKinds record-component type kinds per annotated record, e.g.
   *     {@code missing=ERROR} — the BS-1 observation
   */
  record RoundObservation(
      int roundNumber,
      boolean errorRaised,
      boolean processingOver,
      List<String> annotatedRecords,
      TreeMap<String, String> componentTypeKinds) {

  }

  private final List<RoundObservation> rounds = new ArrayList<>();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
    rounds.add(observe(roundEnvironment, annotations));

    return super.process(annotations, roundEnvironment);
  }

  List<RoundObservation> rounds() {
    return List.copyOf(rounds);
  }

  private RoundObservation observe(
      RoundEnvironment roundEnvironment,
      Set<? extends TypeElement> annotations) {

    List<String> annotatedRecords = new ArrayList<>();
    TreeMap<String, String> componentTypeKinds = new TreeMap<>();

    for (TypeElement annotation : annotations) {
      for (Element annotated : roundEnvironment.getElementsAnnotatedWith(annotation)) {
        if (annotated.getKind() != ElementKind.RECORD) {
          continue;
        }

        TypeElement annotatedRecord = (TypeElement) annotated;
        annotatedRecords.add(annotatedRecord.getQualifiedName().toString());

        annotatedRecord.getRecordComponents().forEach(component ->
            componentTypeKinds.put(
                annotatedRecord.getSimpleName() + "." + component.getSimpleName(),
                component.asType().getKind().name()));
      }
    }

    return new RoundObservation(
        rounds.size() + 1,
        roundEnvironment.errorRaised(),
        roundEnvironment.processingOver(),
        annotatedRecords,
        componentTypeKinds);
  }
}
