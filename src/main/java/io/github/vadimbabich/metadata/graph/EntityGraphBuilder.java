package io.github.vadimbabich.metadata.graph;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.graph.Graph;
import java.io.IOException;
import java.util.Set;
import java.util.function.BiConsumer;

// Guava's @Beta graph type leaks into this SPI signature; see GenerateEntityMetadataMojo.
@SuppressWarnings("UnstableApiUsage")
public interface EntityGraphBuilder {

  Graph<TypeDeclaration<?>> buildEntityGraph(
      BiConsumer<TypeDeclaration<?>, Set<String>> fieldsConsumer) throws IOException;
}
