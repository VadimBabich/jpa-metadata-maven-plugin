package io.github.vadimbabich.entitymetamodel.runtime;

import java.util.Locale;
import java.util.Objects;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

/**
 * Immutable, instance-scoped handle to an entity type. The default instance and any
 * {@link #as(String) aliased copy} are distinct table instances: every name in a statement derives
 * from the instance's alias, never from the class.
 */
public final class EntityRef<E> {

  /**
   * Reserved projected-label separator ({@code <tableAlias>__<column>}); forbidden inside aliases
   * because a duplicate label makes R2DBC's {@code Row.get(String)} silently return the first match.
   */
  static final String PROJECTION_SEPARATOR = "__";

  private final Class<E> entityType;
  private final String alias;

  private EntityRef(Class<E> entityType, String alias) {
    this.entityType = entityType;
    this.alias = alias;
  }

  /** The default instance, aliased with the lower-cased entity simple name. */
  public static <E> EntityRef<E> of(Class<E> entityType) {
    Objects.requireNonNull(entityType, "entityType");

    String defaultAlias = defaultAliasOf(entityType);
    rejectReservedSeparator(defaultAlias, "entity simple name");

    return new EntityRef<>(entityType, defaultAlias);
  }

  /**
   * A distinct instance of the same entity, aliased {@code <thisAlias>_<qualifier>}. Re-aliasing
   * extends the alias rather than replacing the qualifier, so two instances cannot collide.
   */
  public EntityRef<E> as(String qualifier) {
    Objects.requireNonNull(qualifier, "qualifier");
    if (qualifier.isBlank()) {
      throw new IllegalArgumentException("Alias qualifier must not be blank");
    }

    String qualifiedAlias = alias + "_" + qualifier;
    rejectReservedSeparator(qualifiedAlias, "alias");

    return new EntityRef<>(entityType, qualifiedAlias);
  }

  /**
   * A typed property handle. {@code T} is the compile-time contract; the declared raw type is
   * carried for diagnostics only.
   */
  public <T> PropertyRef<E, T> property(String propertyName, Class<?> declaredRawType) {
    Objects.requireNonNull(propertyName, "propertyName");
    if (propertyName.isBlank()) {
      throw new IllegalArgumentException("Property name must not be blank");
    }
    Objects.requireNonNull(declaredRawType, "declaredRawType");

    return new PropertyRef<>(this, propertyName, declaredRawType);
  }

  public Class<E> entityType() {
    return entityType;
  }

  public String alias() {
    return alias;
  }

  /** Resolves the table name through the context — never re-implemented or cached here. */
  public String tableName(RelationalMappingContext mappingContext) {
    Objects.requireNonNull(mappingContext, "mappingContext");

    return mappingContext.getRequiredPersistentEntity(entityType).getTableName().getReference();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof EntityRef<?> otherRef)) {
      return false;
    }

    return entityType.equals(otherRef.entityType) && alias.equals(otherRef.alias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, alias);
  }

  @Override
  public String toString() {
    return "EntityRef[" + entityType.getSimpleName() + " as " + alias + "]";
  }

  private static String defaultAliasOf(Class<?> entityType) {
    return entityType.getSimpleName().toLowerCase(Locale.ROOT);
  }

  private static void rejectReservedSeparator(String candidate, String what) {
    if (candidate.contains(PROJECTION_SEPARATOR)) {
      throw new IllegalArgumentException(
          "The " + what + " '" + candidate + "' contains the reserved separator '"
              + PROJECTION_SEPARATOR + "'");
    }
  }
}
