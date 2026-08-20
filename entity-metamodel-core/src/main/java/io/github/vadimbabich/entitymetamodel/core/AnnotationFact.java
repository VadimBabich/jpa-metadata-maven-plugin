package io.github.vadimbabich.entitymetamodel.core;

import java.util.Map;

/**
 * A declared annotation carried as source-level literals. Effective SQL names resolve at runtime;
 * the model records only what the source says.
 */
public record AnnotationFact(String qualifiedName, Map<String, String> declaredValues) {

  public AnnotationFact {
    TypeRef.requireText(qualifiedName, "qualifiedName");
    declaredValues = Map.copyOf(declaredValues);
  }

  public static AnnotationFact of(String qualifiedName, Map<String, String> declaredValues) {
    return new AnnotationFact(qualifiedName, declaredValues);
  }

  public String simpleName() {
    int lastDot = qualifiedName.lastIndexOf('.');
    return lastDot < 0 ? qualifiedName : qualifiedName.substring(lastDot + 1);
  }
}
