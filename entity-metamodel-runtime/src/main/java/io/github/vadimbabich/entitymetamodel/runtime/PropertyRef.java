package io.github.vadimbabich.entitymetamodel.runtime;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Immutable, typed handle to one entity property on one {@link EntityRef table instance}. Equality
 * is value identity over {@code (entityType, propertyName, alias)}; SQL names resolve on demand
 * through a mapping context and are never cached here.
 */
public final class PropertyRef<E, T> {

  private final EntityRef<E> entity;
  private final String propertyName;
  private final Class<?> declaredRawType;

  PropertyRef(EntityRef<E> entity, String propertyName, Class<?> declaredRawType) {
    this.entity = entity;
    this.propertyName = propertyName;
    this.declaredRawType = declaredRawType;
  }

  /** The Java property name — the drop-in replacement for every {@code nameOf(...)} argument. */
  public String name() {
    return propertyName;
  }

  public EntityRef<E> entity() {
    return entity;
  }

  public Class<?> declaredRawType() {
    return declaredRawType;
  }

  /** Re-anchors this property to another instance of the same entity (self-join reads). */
  public PropertyRef<E, T> of(EntityRef<E> instance) {
    Objects.requireNonNull(instance, "instance");

    return new PropertyRef<>(instance, propertyName, declaredRawType);
  }

  /**
   * Resolves the column name through the context, naming the entity and property when it is not
   * persistent there.
   */
  public String columnName(RelationalMappingContext mappingContext) {
    Objects.requireNonNull(mappingContext, "mappingContext");

    RelationalPersistentEntity<?> persistentEntity =
        mappingContext.getRequiredPersistentEntity(entity.entityType());
    RelationalPersistentProperty persistentProperty =
        persistentEntity.getPersistentProperty(propertyName);

    if (persistentProperty == null) {
      throw new IllegalArgumentException(
          "Property '" + propertyName + "' of entity '" + entity.entityType().getSimpleName()
              + "' is not persistent in this mapping context");
    }

    return persistentProperty.getColumnName().getReference();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PropertyRef<?, ?> otherRef)) {
      return false;
    }

    return entity.equals(otherRef.entity) && propertyName.equals(otherRef.propertyName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, propertyName);
  }

  @Override
  public String toString() {
    return "PropertyRef[" + entity.entityType().getSimpleName() + "." + propertyName
        + " on " + entity.alias() + "]";
  }
}
