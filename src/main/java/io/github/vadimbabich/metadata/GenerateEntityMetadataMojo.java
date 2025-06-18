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
 * A Maven Mojo that generates a static metadata model for JPA entities.
 *
 * <p>This plugin scans the given {@code packageName} for entity classes annotated with
 * {@link org.springframework.data.relational.core.mapping.Table} and generates metadata classes
 * with public static final fields corresponding to the class members annotated with
 * {@link org.springframework.data.relational.core.mapping.Column}.
 *
 * <p>The generated classes mirror the structure of the original JPA model, including:
 * <ul>
 *   <li>Nested classes (inner or static)</li>
 *   <li>Class inheritance hierarchies within the target package</li>
 * </ul>
 *
 * <p>Only top-level and nested entity classes defined directly in the specified {@code packageName}
 * are processed. Classes from project dependencies or outside the given package are not supported.
 *
 * <p>The resulting metadata is emitted as {@code public static final} classes with fields that can
 * be used for reference in compile-time-safe queries, mappings, and schema introspection.
 *
 * <p>Example:
 * <pre>{@code
 * public final class User_ {
 *     public static final Column_ ID = new Column_(User.class, "id");
 *     public static final Column_ NAME = new Column_(User.class, "name");
 * }
 * }</pre>
 *
 * <p>The output is written to the {@code outputDirectory} (defaults to the build output directory),
 * and the generated sources are automatically added to the Maven project's compile source roots.
 *
 * @author Vadim Babich
 * @since 1.0.0
 */
@Mojo(
    name = "generate-metadata",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateEntityMetadataMojo extends AbstractMojo {

  /**
   * The output directory where generated metadata classes will be stored.
   * <p>
   * Defaults to {@code ${project.build.outputDirectory}}.
   */
  @Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}", required = true)
  File outputDirectory;

  /**
   * The root package to scan for JPA entity classes.
   * <p>
   * Example: {@code com.example.model}
   */
  @Parameter(property = "packageName", required = true)
  String packageName;

  /**
   * The Java language level used to parse source files.
   * <p>
   * Valid values are enum constants like {@code JAVA_8}, {@code JAVA_11}, {@code JAVA_17},
   * {@code JAVA_21}, etc.
   */
  @Parameter(property = "javaLanguageLevel", defaultValue = "JAVA_17", required = true)
  JavaLanguageLevel languageLevel;

  /**
   * The relative path to the Java source directory.
   * <p>
   * Defaults to {@code src/main/java}.
   */
  @Parameter(property = "sourceDirectory", defaultValue = "src/main/java/", required = true)
  Path sourceDirectory;

  /**
   * The metadata generator implementation to use.
   * <p>
   * Supported values: {@code r2dbc}, etc.
   */
  @Parameter(property = "entityMetadataGenerator", defaultValue = "r2dbc", required = true)
  String entityMetadataGenerator;

  /**
   * Injected Maven project instance, used to register generated sources.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  MavenProject project;

  /**
   * Executes the metadata generation process as part of the Maven build lifecycle.
   *
   * @throws MojoExecutionException if any step in the metadata generation fails
   */
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

  /**
   * Formats a type hierarchy graph into a readable string representation, showing root nodes and
   * their nested child relationships using indentation.
   *
   * <p>Example output:
   * <pre>
   * • RootClass
   *   ↳ ChildClass
   *     ↳ GrandChildClass
   * </pre>
   *
   * @param graph the graph representing type declarations and their nesting relationships
   * @return a formatted string representing the hierarchy
   */
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