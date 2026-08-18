package io.github.vadimbabich.entitymetamodel.core;

import java.util.List;

/**
 * One declared attribute: name, exact declared type, id-ness and its annotation facts. The model
 * carries facts for every declared attribute; which attributes a generator includes is the
 * inclusion algorithm's decision (ADR-004), applied downstream — never baked in here.
 */
public record AttributeDescriptor(
    String name, TypeRef declaredType, boolean id, List<AnnotationFact> annotations) {

  public AttributeDescriptor {
    TypeRef.requireText(name, "name");
    annotations = List.copyOf(annotations);
  }

  public static AttributeDescriptor of(
      String name, TypeRef declaredType, boolean id, List<AnnotationFact> annotations) {
    return new AttributeDescriptor(name, declaredType, id, annotations);
  }
}
