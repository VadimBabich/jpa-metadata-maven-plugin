package io.github.vadimbabich.metadata.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import io.github.vadimbabich.metadata.parser.MetadataCollector;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Builds a directed graph of {@code @Table} types in a package, with an edge from each type to the
 * nested types it declares. Column fields are collected up the inheritance chain, so a subclass
 * reports its own fields followed by those of its supertypes.
 */
// Builds on Guava's @Beta graph API; see GenerateEntityMetadataMojo.
@SuppressWarnings("UnstableApiUsage")
public class NestedEntityGraphBuilder implements EntityGraphBuilder {

  private final String packageName;
  private final MetadataCollector collector;

  public NestedEntityGraphBuilder(String packageName, MetadataCollector collector) {
    this.packageName = packageName;
    this.collector = collector;
  }

  @Override
  public Graph<TypeDeclaration<?>> buildEntityGraph(
      BiConsumer<TypeDeclaration<?>, Set<String>> fieldsConsumer) throws IOException {

    ImmutableGraph.Builder<TypeDeclaration<?>> graphBuilder = GraphBuilder.directed().immutable();

    for (TypeDeclaration<?> parent : collector.extractAnnotatedClasses(packageName)) {

      graphBuilder.addNode(parent);
      fieldsConsumer.accept(parent, collectColumnFields(parent));

      parent.getMembers().stream()
          .filter(TypeDeclaration.class::isInstance)
          .map(TypeDeclaration.class::cast)
          .filter(this::isAnnotatedWithTable)
          .forEach(child -> {
            graphBuilder.addNode(child);
            graphBuilder.putEdge(parent, child);
          });
    }

    return graphBuilder.build();
  }

  private Set<String> collectColumnFields(TypeDeclaration<?> type) {
    Set<String> fields = new LinkedHashSet<>();
    Set<String> visited = new HashSet<>();
    collectFieldsRecursive(type, fields, visited);
    return fields;
  }

  private void collectFieldsRecursive(TypeDeclaration<?> type, Set<String> fields,
      Set<String> visited) {
    String fqn = getFullyQualifiedName(type);
    if (!visited.add(fqn)) {
      return;
    }

    fields.addAll(collector.collectColumnAnnotatedFieldNames(type));

    findSuperType(type).ifPresent(superType -> collectFieldsRecursive(superType, fields, visited));
  }

  private Optional<TypeDeclaration<?>> findSuperType(TypeDeclaration<?> type) {
    if (!(type instanceof ClassOrInterfaceDeclaration decl)) {
      return Optional.empty();
    }

    return decl.getExtendedTypes().stream()
        .findFirst()
        .flatMap(this::resolveTypeDeclarationSafe);
  }

  private Optional<TypeDeclaration<?>> resolveTypeDeclarationSafe(ClassOrInterfaceType type) {
    try {
      return resolveTypeDeclaration(type);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  private Optional<TypeDeclaration<?>> resolveTypeDeclaration(ClassOrInterfaceType type)
      throws IOException {
    String fqn = resolveFullyQualifiedName(type);
    return collector.extractClasses("", t -> getFullyQualifiedName(t).equals(fqn))
        .stream()
        .findFirst();
  }

  // JavaParser symbol resolution is not configured, so type.resolve() fails for anything it cannot
  // see. Fall back to matching the simple name across the sources, then to assuming the supertype
  // sits in the scanned package. A wrong guess costs the inherited columns, not the build.
  private String resolveFullyQualifiedName(ClassOrInterfaceType type) {
    try {
      return type.resolve().asReferenceType().getQualifiedName();
    } catch (Exception ex) {
      try {
        return collector.extractClasses("", t -> t.getNameAsString().equals(type.getNameAsString()))
            .stream()
            .map(this::getFullyQualifiedName)
            .findFirst()
            .orElse(packageName + "." + type.getNameAsString());
      } catch (IOException e) {
        return packageName + "." + type.getNameAsString();
      }
    }
  }

  private String getFullyQualifiedName(TypeDeclaration<?> type) {
    return type.findCompilationUnit()
        .flatMap(CompilationUnit::getPackageDeclaration)
        .map(pkg -> pkg.getNameAsString() + "." + type.getNameAsString())
        .orElse(type.getNameAsString());
  }

  private boolean isAnnotatedWithTable(BodyDeclaration<?> decl) {
    return decl.getAnnotationByClass(Table.class).isPresent();
  }
}
