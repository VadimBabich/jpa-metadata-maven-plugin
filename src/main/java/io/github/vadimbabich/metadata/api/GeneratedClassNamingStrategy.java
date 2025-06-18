package io.github.vadimbabich.metadata.api;

import java.util.function.UnaryOperator;

/**
 * Strategy interface for determining the target class name derived from an entity class
 * declaration.
 *
 * @author Vadim Babich
 */
public interface GeneratedClassNamingStrategy extends UnaryOperator<String> {

  /**
   * Returns the target class name corresponding to the given entity class declaration.
   *
   * @param entityClassName the name of the entity class
   * @return the computed target class name
   */
  default String getMetadataClassName(String entityClassName){
    return apply(entityClassName);
  }
}
