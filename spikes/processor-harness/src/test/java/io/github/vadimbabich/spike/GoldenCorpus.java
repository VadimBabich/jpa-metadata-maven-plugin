package io.github.vadimbabich.spike;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Read-only view of the integration test's entities and committed golden corpus.
 *
 * <p>Everything is referenced in place. Copying the corpus into the spike would create a second
 * copy free to diverge from the one the shape freeze is measured against.
 */
final class GoldenCorpus {

  private static final String CONSUMER_PATH = "jpa-metadata-maven-plugin/src/it/simple-consumer";
  private static final String METAMODEL_PACKAGE_PATH = "com/example/model";

  private final Path consumerRoot;

  private GoldenCorpus(Path consumerRoot) {
    this.consumerRoot = consumerRoot;
  }

  /**
   * Walks up from the working directory so the spike runs the same from Maven and from an IDE,
   * neither of which agrees on what the working directory should be.
   */
  static GoldenCorpus locate() {
    Path candidate = Path.of("").toAbsolutePath();

    while (candidate != null) {
      Path consumerRoot = candidate.resolve(CONSUMER_PATH);
      if (Files.isDirectory(consumerRoot)) {
        return new GoldenCorpus(consumerRoot);
      }
      candidate = candidate.getParent();
    }

    throw new IllegalStateException(
        "No " + CONSUMER_PATH + " above " + Path.of("").toAbsolutePath()
            + " — the spike must run from inside the repository");
  }

  /** The {@code @Table} records the processor reads. */
  List<Path> entitySources() {
    Path modelRoot = consumerRoot.resolve("src/main/java").resolve(METAMODEL_PACKAGE_PATH);

    return List.of(
        modelRoot.resolve("User.java"),
        modelRoot.resolve("UserAttribute.java"));
  }

  /**
   * Golden support classes supplied as pre-existing compilation inputs. The 1.x plugin generates
   * these; the spike deliberately does not, so the emitted metamodels still resolve.
   */
  List<Path> supportSources() {
    Path expectedRoot = expectedRoot();

    return List.of(
        expectedRoot.resolve("org/springframework/data/relational/core/sql/Column_.java"),
        expectedRoot.resolve("org/springframework/data/r2dbc/config/"
            + "StaticR2dbcEntityTemplateAccessor_.java"));
  }

  /** Golden metamodels the processor must reproduce, keyed by path relative to the source root. */
  Map<String, Path> goldenMetamodels() {
    Path expectedRoot = expectedRoot();
    Path metamodelRoot = expectedRoot.resolve(METAMODEL_PACKAGE_PATH);

    try (Stream<Path> files = Files.list(metamodelRoot)) {
      Map<String, Path> byRelativePath = new TreeMap<>();

      files.filter(file -> file.getFileName().toString().endsWith(".java"))
          .forEach(file -> byRelativePath.put(expectedRoot.relativize(file).toString(), file));

      return Map.copyOf(byRelativePath);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the golden corpus at " + metamodelRoot, e);
    }
  }

  private Path expectedRoot() {
    return consumerRoot.resolve("expected");
  }
}
