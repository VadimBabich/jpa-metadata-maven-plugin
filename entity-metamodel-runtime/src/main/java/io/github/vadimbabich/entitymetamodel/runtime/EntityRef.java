package io.github.vadimbabich.entitymetamodel.runtime;

import java.util.Locale;
import java.util.Objects;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

/**
 * Immutable, instance-scoped handle to an entity type. Two instances of the same entity — the
 * default and any {@link #as(String) aliased copy} — are distinct table instances with distinct
 * identity; every name in a statement derives from the instance's alias, never the class.
 */
public final class EntityRef<E> {

  /**
   * Reserved projected-label separator ({@code <tableAlias>__<column>}); forbidden inside
   * aliases because a duplicate label makes R2DBC's {@code Row.get(String)} return the first
   * match silently.
   */
  static final String PROJECTION_SEPARATOR = "__";

  private final Class<E> entityType;
  private final String alias;

  private EntityRef(Class<E> entityType, String alias) {
    this.entityType = entityType;
    this.alias = alias;
  }

  /** Creates the default instance; its alias is the lower-cased entity simple name. */
  public static <E> EntityRef<E> of(Class<E> entityType) {
    Objects.requireNonNull(entityType, "entityType");

    String defaultAlias = defaultAliasOf(entityType);
    rejectReservedSeparator(defaultAlias, "entity simple name");

    return new EntityRef<>(entityType, defaultAlias);
  }

  /**
   * Creates a distinct instance of the same entity, aliased {@code <defaultAlias>_<qualifier>} —
   * the second table instance in a self-join.
   */
  public EntityRef<E> as(String qualifier) {
    Objects.requireNonNull(qualifier, "qualifier");
    if (qualifier.isBlank()) {
      throw new IllegalArgumentException("Alias qualifier must not be blank");
    }
    rejectReservedSeparator(qualifier, "alias qualifier");

    return new EntityRef<>(entityType, defaultAliasOf(entityType) + "_" + qualifier);
  }

  /**
   * Creates a typed property handle. The declared raw type is carried for diagnostics and future
   * consumers; the ref's {@code T} parameter is the compile-time contract.
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

  /**
   * Resolves the table name through the given mapping context — names are always the context's
   * answer, never re-implemented or cached here.
   */
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
