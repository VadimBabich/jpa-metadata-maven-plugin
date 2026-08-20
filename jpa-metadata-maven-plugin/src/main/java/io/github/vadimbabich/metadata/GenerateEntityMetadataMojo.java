package io.github.vadimbabich.metadata;


import com.github.javaparser.ast.body.TypeDeclaration;
import com.google.common.graph.Graph;
import io.github.vadimbabich.metadata.api.EntityMetadataGenerator;
import io.github.vadimbabich.metadata.api.JavaLanguageLevel;
import io.github.vadimbabich.metadata.graph.EntityGraphBuilder;
import io.github.vadimbabich.metadata.graph.NestedEntityGraphBuilder;
import io.github.vadimbabich.metadata.parser.MetadataCollector;
import io.github.vadimbabich.metadata.parser.io.FileSystemJavaFileFinder;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Generates static metamodel classes from Spring Data Relational entities, so that column
 * references are checked at compile time instead of being written as strings.
 *
 * <p>Only {@code @Table} types declared in {@code packageName} are processed; entities from
 * dependencies or other packages are not.
 *
 * @author Vadim Babich
 * @since 1.0.0
 */
// Guava's com.google.common.graph is @Beta and may change across Guava majors. The graph type
// also appears in this plugin's SPI; replacing it is owned by the API/SPI governance workstream.
@SuppressWarnings("UnstableApiUsage")
@Mojo(
    name = "generate-metadata",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateEntityMetadataMojo extends AbstractMojo {

  /**
   * Directory the generated metadata classes are written to. It is registered as a compile source
   * root, so generated sources are compiled with the rest of the project.
   */
  @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/metamodel", required = true)
  File outputDirectory;

  /**
   * Root package to scan for entity classes, for example {@code com.example.model}.
   */
  @Parameter(property = "packageName", required = true)
  String packageName;

  /**
   * Java language level used to parse the sources.
   */
  @Parameter(property = "javaLanguageLevel", defaultValue = "JAVA_17", required = true)
  JavaLanguageLevel languageLevel;

  /**
   * Source root to scan, resolved against the project base directory.
   */
  @Parameter(property = "sourceDirectory", defaultValue = "src/main/java", required = true)
  Path sourceDirectory;

  /**
   * Name of the generator implementation to use. Only {@code r2dbc} ships with the plugin.
   */
  @Parameter(property = "entityMetadataGenerator", defaultValue = "r2dbc")
  String entityMetadataGenerator;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    Log log = getLog();
    try {
      logStart();

      Path sourceRoot = resolveSourceRoot();
      MetadataCollector collector = createMetadataCollector(log, sourceRoot);
      EntityMetadataGenerator metadataGenerator = resolveMetadataGenerator(log);
      EntityGraphBuilder graphBuilder = new NestedEntityGraphBuilder(packageName, collector);

      Map<TypeDeclaration<?>, Set<String>> entityFieldsMap = new HashMap<>();
      Graph<TypeDeclaration<?>> typeGraph = graphBuilder.buildEntityGraph(entityFieldsMap::put);

      metadataGenerator.generateMetadataClasses(typeGraph, entityFieldsMap::get);

      project.addCompileSourceRoot(getAbsoluteOutputPath());
      logSummary(typeGraph);

    } catch (Exception e) {
      log.error("Metadata generation failed: " + e.getMessage(), e);
      throw new MojoExecutionException("Error generating metadata", e);
    }
  }

  MetadataCollector createMetadataCollector(Log log, Path sourceRoot) {
    return new MetadataCollector(new FileSystemJavaFileFinder(), sourceRoot, languageLevel, log);
  }

  EntityMetadataGenerator resolveMetadataGenerator(Log log) {
    return new MetadataGeneratorFactory(
        entityMetadataGenerator,
        outputDirectory,
        log,
        entityClassName -> entityClassName + "_"
    ).resolve();
  }

  private Path resolveSourceRoot() {
    return Objects.requireNonNull(project.getBasedir(),
            "Maven project base directory is null. "
                + "Ensure the project is properly initialized and not run in an unsupported context.")
        .toPath()
        .resolve(sourceDirectory);
  }

  private String getAbsoluteOutputPath() {
    return Objects.requireNonNull(outputDirectory,
            "Missing output directory. "
                + "Please set 'outputDirectory' or ensure the project build output directory is configured.")
        .getAbsolutePath();
  }

  private void logStart() {
    getLog().info(String.format(
        "Generating metadata for '%s' package with language level '%s'", packageName,
        languageLevel));
  }

  private void logSummary(Graph<TypeDeclaration<?>> graph) {
    getLog().info(String.format(
        "Generated metadata for %d entity classes into: '%s'%nIncluded entities:\n%s",
        graph.nodes().size(),
        outputDirectory,
        formatGraphHierarchy(graph)
    ));
  }

  private String formatGraphHierarchy(Graph<TypeDeclaration<?>> graph) {
    StringBuilder sb = new StringBuilder();

    Set<TypeDeclaration<?>> roots = graph.nodes().stream()
        .filter(node -> graph.predecessors(node).isEmpty())
        .collect(Collectors.toSet());

    for (TypeDeclaration<?> root : roots) {
      formatNodeHierarchy(graph, root, 0, new HashSet<>(), sb);
    }

    return sb.toString();
  }

  private void formatNodeHierarchy(Graph<TypeDeclaration<?>> graph,
      TypeDeclaration<?> node,
      int depth,
      Set<TypeDeclaration<?>> visited,
      StringBuilder sb) {
    if (!visited.add(node)) {
      return;
    }

    String indent = depth == 0
        ? "\t• "
        : "\t  ".repeat(depth) + "↳ ";

    sb.append(indent).append(node.getNameAsString()).append("\n");

    for (TypeDeclaration<?> child : graph.successors(node)) {
      formatNodeHierarchy(graph, child, depth + 1, visited, sb);
    }
  }

}
