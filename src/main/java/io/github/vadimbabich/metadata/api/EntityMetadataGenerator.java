package io.github.vadimbabich.metadata.api;


import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.graph.Graph;
import java.io.IOException;
import java.util.Set;
import java.util.function.Function;

/**
 * Interface for generating metadata classes from Java entity declarations.
 *
 * @author Vadim Babich
 */
public interface EntityMetadataGenerator {

  /**
   * Generates metadata source files for the given root entity and its related types.
   *
   * @param graph                the graph of related entity declarations
   * @param entityFieldsResolver function to extract field names from entity types
   * @throws IOException if the generation fails due to I/O issues
   */
  void generateMetadataClasses(Graph<TypeDeclaration<?>> graph,
      Function<TypeDeclaration<?>, Set<String>> entityFieldsResolver) throws IOException;
}
