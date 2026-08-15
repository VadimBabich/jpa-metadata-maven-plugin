package io.github.vadimbabich.metadata.api;

import java.util.function.UnaryOperator;

/**
 * Maps an entity class name to the name of its generated metamodel class.
 *
 * @author Vadim Babich
 */
public interface GeneratedClassNamingStrategy extends UnaryOperator<String> {

  default String getMetadataClassName(String entityClassName){
    return apply(entityClassName);
  }
}
