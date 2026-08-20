package io.github.vadimbabich.metadata.parser.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Locates Java sources under a root directory.
 *
 * @author Vadim Babich
 */
@FunctionalInterface
public interface JavaFileFinder {

  /**
   * The returned stream holds an open directory handle; the caller must close it.
   */
  Stream<Path> findJavaFiles(Path root) throws IOException;
}
