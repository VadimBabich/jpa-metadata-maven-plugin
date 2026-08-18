package io.github.vadimbabich.entitymetamodel.core;

import java.util.Map;

/**
 * A declared annotation as a fact: identity is (namespace, simple name) — the FQN — and the
 * declared attribute values are carried as source-level literals. Resolution of effective SQL
 * names stays runtime-side; the model records what the source says, nothing more.
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
