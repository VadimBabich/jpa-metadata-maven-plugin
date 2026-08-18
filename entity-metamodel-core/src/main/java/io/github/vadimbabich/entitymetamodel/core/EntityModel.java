package io.github.vadimbabich.entitymetamodel.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The frontend's complete payload. Entities carry a defined total order — qualified-name
 * lexicographic — so any output that aggregates across entities is deterministic by construction,
 * never by accident of discovery order (model spec §5.4).
 */
public record EntityModel(List<EntityDescriptor> entities) {

  public EntityModel {
    List<EntityDescriptor> sorted = new ArrayList<>(entities);
    sorted.sort(Comparator.comparing(EntityDescriptor::qualifiedName));
    entities = List.copyOf(sorted);
  }

  public static EntityModel of(List<EntityDescriptor> entities) {
    return new EntityModel(entities);
  }
}
