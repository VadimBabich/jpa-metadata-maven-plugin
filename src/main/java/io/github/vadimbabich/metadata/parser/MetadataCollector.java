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
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Parses Java sources and extracts the entity mapping metadata the generators work from.
 *
 * <p>Sources are read with JavaParser rather than reflection, so entities need not be compiled and
 * their declaration order is preserved.
 *
 * @author Vadim Babich
 */
public class MetadataCollector {

  private final Log log;
  private final Path sourceDirectory;
  private final LanguageLevel languageLevel;
  private final JavaParser javaParser;
  private final JavaFileFinder javaFileFinder;

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
   * Finds the {@code @Table} types declared under {@code packageName}.
   *
   * @throws IOException if the sources cannot be read
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
   * Returns the {@code @Column} field names of an entity, in declaration order.
   */
  public Set<String> collectColumnAnnotatedFieldNames(TypeDeclaration<?> entity) {
    if (entity instanceof RecordDeclaration recordDeclaration) {
      return collectFields(recordDeclaration);
    } else if (entity instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
      return collectFields(classOrInterfaceDeclaration);
    }
    return Collections.emptySet();
  }

  private Set<String> collectFields(RecordDeclaration declaration) {
    return declaration.getParameters().stream()
        .filter(parameter -> parameter.getAnnotationByName("Column").isPresent())
        .map(NodeWithSimpleName::getNameAsString)
        .collect(toUnmodifiableOrderedSet());
  }

  private Set<String> collectFields(ClassOrInterfaceDeclaration declaration) {
    return declaration.getFields().stream()
        .filter(field -> field.getAnnotationByClass(Column.class).isPresent())
        .map(field -> field.getVariable(0).getNameAsString())
        .collect(toUnmodifiableOrderedSet());
  }

  // Generated output must be byte-identical between runs, so encounter order has to survive
  // collection; toUnmodifiableSet gives no such guarantee, and toCollection alone would leave the
  // set mutable.
  private static Collector<String, ?, Set<String>> toUnmodifiableOrderedSet() {
    return Collectors.collectingAndThen(
        Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet);
  }

  // A file that fails to parse is skipped rather than failing the build: one malformed source
  // should not block metadata generation for the rest of the package.
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

  private Set<TypeDeclaration<?>> extractTypes(CompilationUnit cu,
      Predicate<TypeDeclaration<?>> filter) {
    return cu.findAll(TypeDeclaration.class).stream()
        .map(type -> (TypeDeclaration<?>) type)
        .filter(filter)
        .collect(Collectors.toSet());
  }

  private LanguageLevel getLanguageLevel(JavaLanguageLevel languageLevel, Log log) {
    try {
      return LanguageLevel.valueOf(languageLevel.name());
    } catch (IllegalArgumentException e) {
      log.error(format("Invalid Java language level '%s', available levels: %s",
          languageLevel, Arrays.toString(LanguageLevel.values())), e);
      throw e;
    }
  }

  private Path toPathFromPackage(String packageName) {
    return sourceDirectory.resolve(packageName.replace(".", File.separator));
  }
}
