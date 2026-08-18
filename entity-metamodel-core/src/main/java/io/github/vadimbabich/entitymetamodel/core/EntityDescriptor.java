package io.github.vadimbabich.entitymetamodel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a frontend knows about one entity type: identity, declared table name, own
 * attributes in declaration order, supertype contributions in extends order, and nested entities
 * mirroring source nesting. Immutable; built once by a frontend, read by generators.
 */
public record EntityDescriptor(
    String qualifiedName,
    TypeKind kind,
    String tableName,
    List<AttributeDescriptor> attributes,
    List<SuperTypeContribution> superTypes,
    List<EntityDescriptor> nestedEntities) {

  public EntityDescriptor {
    TypeRef.requireText(qualifiedName, "qualifiedName");
    attributes = List.copyOf(attributes);
    superTypes = List.copyOf(superTypes);
    nestedEntities = List.copyOf(nestedEntities);
  }

  public static Builder builder(String qualifiedName, TypeKind kind) {
    return new Builder(qualifiedName, kind);
  }

  public static final class Builder {

    private final String qualifiedName;
    private final TypeKind kind;
    private String tableName = "";
    private final List<AttributeDescriptor> attributes = new ArrayList<>();
    private final List<SuperTypeContribution> superTypes = new ArrayList<>();
    private final List<EntityDescriptor> nestedEntities = new ArrayList<>();

    private Builder(String qualifiedName, TypeKind kind) {
      this.qualifiedName = qualifiedName;
      this.kind = kind;
    }

    public Builder tableName(String declaredTableName) {
      this.tableName = declaredTableName;
      return this;
    }

    public Builder attribute(AttributeDescriptor attribute) {
      this.attributes.add(attribute);
      return this;
    }

    public Builder attributes(List<AttributeDescriptor> declarationOrdered) {
      this.attributes.addAll(declarationOrdered);
      return this;
    }

    public Builder superType(SuperTypeContribution contribution) {
      this.superTypes.add(contribution);
      return this;
    }

    public Builder nestedEntity(EntityDescriptor nested) {
      this.nestedEntities.add(nested);
      return this;
    }

    public EntityDescriptor build() {
      return new EntityDescriptor(
          qualifiedName, kind, tableName, attributes, superTypes, nestedEntities);
    }
  }
}
