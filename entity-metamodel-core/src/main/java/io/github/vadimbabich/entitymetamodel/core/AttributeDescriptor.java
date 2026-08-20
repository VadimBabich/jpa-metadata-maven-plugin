package io.github.vadimbabich.entitymetamodel.core;

import java.util.List;

/**
 * One declared attribute. Facts are carried for every attribute; which ones a generator includes is
 * the inclusion algorithm's decision, applied downstream.
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
