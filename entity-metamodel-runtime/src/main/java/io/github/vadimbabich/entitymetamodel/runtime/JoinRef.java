package io.github.vadimbabich.entitymetamodel.runtime;

import java.util.Objects;

/**
 * Immutable, typed join descriptor: one declared relationship from a source entity's FK property
 * to a target entity's property. Pure data — a JoinRef names a relationship, never an instance;
 * traversing the same relationship to a second or third table instance is the builder's job,
 * composed from {@link PropertyRef#of(EntityRef) re-anchoring}.
 *
 * <p>The shared value type {@code V} makes FK/target type compatibility a compile-time property
 * of generated code: a mismatched pair does not compile.
 */
public final class JoinRef<S, T> {

  private final PropertyRef<S, ?> source;
  private final PropertyRef<T, ?> target;

  private JoinRef(PropertyRef<S, ?> source, PropertyRef<T, ?> target) {
    this.source = source;
    this.target = target;
  }

  public static <S, T, V> JoinRef<S, T> of(PropertyRef<S, V> source, PropertyRef<T, V> target) {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");

    return new JoinRef<>(source, target);
  }

  /** The FK property on the declaring entity. */
  public PropertyRef<S, ?> source() {
    return source;
  }

  /** The referenced property on the target's default instance; re-anchor per instance for N-way joins. */
  public PropertyRef<T, ?> target() {
    return target;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JoinRef<?, ?> otherRef)) {
      return false;
    }

    return source.equals(otherRef.source) && target.equals(otherRef.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, target);
  }

  @Override
  public String toString() {
    return "JoinRef[" + source + " -> " + target + "]";
  }
}
