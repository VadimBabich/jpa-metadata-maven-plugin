package io.github.vadimbabich.metadata.parser.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Functional interface for finding Java source files in a file system.
 *
 * @author Vadim Babich
 */
@FunctionalInterface
public interface JavaFileFinder {

  /**
   * Returns a stream of Java source file paths under the given root directory.
   *
   * @param root the root directory to search
   * @return a stream of Java file paths
   * @throws IOException if an I/O error occurs
   */
  Stream<Path> findJavaFiles(Path root) throws IOException;
}
