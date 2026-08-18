package io.github.vadimbabich.entitymetamodel.core;

import java.util.List;

/**
 * The attributes one supertype contributes to an entity, attributed to their declaring type. The
 * model carries hierarchy structure; flattening (and its ordering) is a per-generator choice.
 */
public record SuperTypeContribution(String qualifiedName, List<AttributeDescriptor> attributes) {

  public SuperTypeContribution {
    TypeRef.requireText(qualifiedName, "qualifiedName");
    attributes = List.copyOf(attributes);
  }

  public static SuperTypeContribution of(
      String qualifiedName, List<AttributeDescriptor> attributes) {
    return new SuperTypeContribution(qualifiedName, attributes);
  }
}
