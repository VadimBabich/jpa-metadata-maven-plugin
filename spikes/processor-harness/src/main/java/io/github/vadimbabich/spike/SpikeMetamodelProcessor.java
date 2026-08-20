package io.github.vadimbabich.spike;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Minimal JSR-269 processor: emits the 1.x metamodel shape for {@code @Table} records and nothing
 * else. Deliberately has no metadata model, no naming strategy and no options — it answers the
 * spike's three questions and is thrown away.
 */
@SupportedAnnotationTypes("org.springframework.data.relational.core.mapping.Table")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class SpikeMetamodelProcessor extends AbstractProcessor {

  private static final String REFERENCES_ANNOTATION =
      "io.github.vadimbabich.spike.SpikeReferences";
  private static final String ID_ANNOTATION = "org.springframework.data.annotation.Id";

  private final MetamodelRenderer renderer = new MetamodelRenderer();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
    if (roundEnvironment.processingOver()) {
      return true;
    }

    for (TypeElement annotation : annotations) {
      for (Element annotated : roundEnvironment.getElementsAnnotatedWith(annotation)) {
        if (annotated.getKind() == ElementKind.RECORD) {
          emitFor((TypeElement) annotated);
        }
      }
    }

    return true;
  }

  private void emitFor(TypeElement entity) {
    AnnotationMirror reference = referencesAnnotationOf(entity);

    if (reference == null) {
      emitMetamodel(entity);
    } else {
      emitReferenceStandIn(entity, reference);
    }
  }

  private void emitMetamodel(TypeElement entity) {
    String packageName = processingEnv.getElementUtils()
        .getPackageOf(entity)
        .getQualifiedName()
        .toString();
    String entityName = entity.getSimpleName().toString();

    // getRecordComponents is specified to return declaration order; getEnclosedElements is not.
    List<String> propertyNames = entity.getRecordComponents().stream()
        .map(component -> component.getSimpleName().toString())
        .toList();

    String source = renderer.render(packageName, entityName, propertyNames);

    writeSourceFile(entity, packageName + "." + entityName + "_", source);
  }

  /**
   * Cross-entity mode: reads the reference target's {@code @Id} property through the annotation —
   * the read that Gradle's isolating category cannot see, which is what round 4 measures.
   */
  private void emitReferenceStandIn(TypeElement entity, AnnotationMirror reference) {
    TypeMirror targetType = referenceTargetOf(reference);

    if (targetType == null || targetType.getKind() == TypeKind.ERROR) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "@SpikeReferences target of " + entity.getSimpleName() + " does not resolve",
          entity);
      return;
    }

    TypeElement target = (TypeElement) processingEnv.getTypeUtils().asElement(targetType);
    VariableElement idField = idFieldOf(target);

    if (idField == null) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "@SpikeReferences target " + target.getQualifiedName() + " of "
              + entity.getSimpleName() + " has no @Id property",
          entity);
      return;
    }

    String packageName = processingEnv.getElementUtils()
        .getPackageOf(entity)
        .getQualifiedName()
        .toString();
    String entityName = entity.getSimpleName().toString();

    String source = renderer.renderReferenceStandIn(
        packageName,
        entityName,
        target.getQualifiedName().toString(),
        idField.getSimpleName().toString(),
        idField.asType().toString());

    writeSourceFile(entity, packageName + "." + entityName + "_", source);
  }

  private AnnotationMirror referencesAnnotationOf(TypeElement entity) {
    for (AnnotationMirror mirror : entity.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(REFERENCES_ANNOTATION)) {
        return mirror;
      }
    }

    return null;
  }

  private TypeMirror referenceTargetOf(AnnotationMirror reference) {
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> attribute
        : reference.getElementValues().entrySet()) {

      if (attribute.getKey().getSimpleName().contentEquals("target")
          && attribute.getValue().getValue() instanceof TypeMirror targetType) {
        return targetType;
      }
    }

    return null;
  }

  private VariableElement idFieldOf(TypeElement target) {
    for (VariableElement field : ElementFilter.fieldsIn(target.getEnclosedElements())) {
      for (AnnotationMirror mirror : field.getAnnotationMirrors()) {
        if (mirror.getAnnotationType().toString().equals(ID_ANNOTATION)) {
          return field;
        }
      }
    }

    return null;
  }

  private void writeSourceFile(TypeElement entity, String metamodelName, String source) {
    try {
      // Exactly one originating element. Gradle's isolating category rejects a processor that
      // passes more than one, and round 4 measures whether isolating is even admissible.
      JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(metamodelName, entity);

      try (OutputStream bytes = sourceFile.openOutputStream()) {
        bytes.write(source.getBytes(UTF_8));
      }
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Cannot write " + metamodelName + ": " + e.getMessage(),
          entity);
    }
  }
}
