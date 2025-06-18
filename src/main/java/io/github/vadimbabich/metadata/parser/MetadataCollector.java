package io.github.vadimbabich.metadata.parser;

import static java.lang.String.format;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import io.github.vadimbabich.metadata.api.JavaLanguageLevel;
import io.github.vadimbabich.metadata.parser.io.JavaFileFinder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The {@code MetadataCollector} class is responsible for parsing Java source files, identifying
 * annotated classes, and extracting metadata related to database entity mappings.
 *
 * @author Vadim Babich
 */
public class MetadataCollector {

  private final Log log;
  private final Path sourceDirectory;
  private final LanguageLevel languageLevel;
  private final JavaParser javaParser;
  private final JavaFileFinder javaFileFinder;

  /**
   * Constructs a {@code MetadataCollector} instance.
   *
   * @param languageLevel the Java language level to use for parsing.
   * @param log           the logging instance for debugging and error reporting.
   */
  public MetadataCollector(JavaFileFinder javaFileFinder, Path sourceDirectory,
      JavaLanguageLevel languageLevel, Log log) {
    this.log = log;
    this.sourceDirectory = sourceDirectory;
    this.languageLevel = getLanguageLevel(languageLevel, log);
    this.javaFileFinder = javaFileFinder;
    this.javaParser = new JavaParser(new ParserConfiguration()
        .setLanguageLevel(this.languageLevel));
  }

  /**
   * Scans a given package for Java classes annotated with {@code @Table} and returns a set of
   * annotated class declarations.
   *
   * @param packageName the package to scan for annotated classes.
   * @return a set of annotated class declarations.
   * @throws IOException if an error occurs while reading files.
   */
  public Set<TypeDeclaration<?>> extractAnnotatedClasses(String packageName) throws IOException {
    log.debug(format("Collecting entities in package '%s' with language level '%s'", packageName,
        languageLevel));

    return extractClasses(packageName, type -> type.isAnnotationPresent(Table.class));
  }

  public Set<TypeDeclaration<?>> extractClasses(String packageName,
      Predicate<TypeDeclaration<?>> filter) throws IOException {

    Path startPath = toPathFromPackage(packageName);

    log.debug(format("Scanning classes in path: '%s'", startPath));

    try (Stream<Path> files = javaFileFinder.findJavaFiles(startPath)) {

      return files.map(this::parseJavaFile)
          .flatMap(Optional::stream)
          .flatMap(cu -> extractTypes(cu, filter).stream())
          .collect(Collectors.toCollection(LinkedHashSet::new));

    } catch (IOException e) {
      log.error(format("Error while scanning classes in package '%s'", packageName), e);
      throw e;
    }
  }

  /**
   * Collects field names that are annotated with {@code @Column} from the given entity.
   *
   * @param entity the entity to process.
   * @return a set of field names annotated with {@code @Column}.
   */
  public Set<String> collectColumnAnnotatedFieldNames(TypeDeclaration<?> entity) {
    if (entity instanceof RecordDeclaration recordDeclaration) {
      return collectFields(recordDeclaration);
    } else if (entity instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
      return collectFields(classOrInterfaceDeclaration);
    }
    return Collections.emptySet();
  }

  /**
   * Extracts field names from a {@link RecordDeclaration} that are annotated with {@code @Column}.
   *
   * @param declaration the record declaration.
   * @return a set of field names.
   */
  private Set<String> collectFields(RecordDeclaration declaration) {
    return declaration.getParameters().stream()
        .filter(parameter -> parameter.getAnnotationByName("Column").isPresent())
        .map(NodeWithSimpleName::getNameAsString)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Extracts field names from a {@link ClassOrInterfaceDeclaration} that are annotated with
   * {@code @Column}.
   *
   * @param declaration the class or interface declaration.
   * @return a set of field names.
   */
  private Set<String> collectFields(ClassOrInterfaceDeclaration declaration) {
    return declaration.getFields().stream()
        .filter(field -> field.getAnnotationByClass(Column.class).isPresent())
        .map(field -> field.getVariable(0).getNameAsString())
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Parses a Java file into a {@link CompilationUnit}.
   *
   * @param path the file path.
   * @return an {@link Optional} containing the parsed {@link CompilationUnit}, or empty if parsing
   * fails.
   */
  private Optional<CompilationUnit> parseJavaFile(Path path) {
    try {
      log.debug(format("Parsing file: '%s'", path));
      ParseResult<CompilationUnit> result = javaParser.parse(path);
      result.getProblems().forEach(problem -> log.warn("Parsing issue: " + problem));
      return result.getResult();
    } catch (Exception e) {
      log.error(format("Error parsing file '%s'", path), e);
      return Optional.empty();
    }
  }

  /**
   * Extracts classes and members annotated with {@code @Table} from a {@link CompilationUnit}.
   *
   * @param cu the compilation unit.
   * @return a set of annotated class declarations.
   */
  private Set<TypeDeclaration<?>> extractTypes(CompilationUnit cu,
      Predicate<TypeDeclaration<?>> filter) {
    return cu.findAll(TypeDeclaration.class).stream()
        .map(type -> (TypeDeclaration<?>) type)
        .filter(filter)
        .collect(Collectors.toSet());
  }

  /**
   * Converts a string representation of a Java language level to a {@link LanguageLevel} enum.
   *
   * @param languageLevel the language level string.
   * @param log           the logging instance.
   * @return the corresponding {@link LanguageLevel} enum value.
   * @throws IllegalArgumentException if the provided language level is invalid.
   */
  private LanguageLevel getLanguageLevel(JavaLanguageLevel languageLevel, Log log) {
    try {
      return LanguageLevel.valueOf(languageLevel.name());
    } catch (IllegalArgumentException e) {
      log.error(format("Invalid Java language level '%s', available levels: %s",
          languageLevel, Arrays.toString(LanguageLevel.values())), e);
      throw e;
    }
  }

  /**
   * Converts a package name into a normalized file system path.
   *
   * @param packageName the package name.
   * @return the corresponding path as a string.
   */
  private Path toPathFromPackage(String packageName) {
    return sourceDirectory.resolve(packageName.replace(".", File.separator));
  }
}