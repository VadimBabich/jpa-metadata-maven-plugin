package io.github.vadimbabich.metadata.api;


import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.graph.Graph;
import java.io.IOException;
import java.util.Set;
import java.util.function.Function;

/**
 * Writes the metamodel sources for a graph of entity declarations. This is the extension point for
 * supporting a persistence backend other than R2DBC.
 *
 * @author Vadim Babich
 */
// Guava's @Beta graph type leaks into this SPI signature; see GenerateEntityMetadataMojo.
@SuppressWarnings("UnstableApiUsage")
public interface EntityMetadataGenerator {

  /**
   * Field names from {@code entityFieldsResolver} arrive in declaration order, and implementations
   * must emit them in that order: regenerating unchanged sources has to be byte-identical.
   */
  void generateMetadataClasses(Graph<TypeDeclaration<?>> graph,
      Function<TypeDeclaration<?>, Set<String>> entityFieldsResolver) throws IOException;
}
