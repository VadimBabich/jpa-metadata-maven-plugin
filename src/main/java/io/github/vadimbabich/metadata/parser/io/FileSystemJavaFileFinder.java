package io.github.vadimbabich.metadata.parser.io;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;

/**
 * Finds all Java source files (*.java) under a given root directory.
 * Uses  and a {@link PathMatcher} with a glob pattern.
 *
 * <p>The caller is responsible for closing the returned stream.
 */
public class FileSystemJavaFileFinder implements JavaFileFinder {

  private final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.java");

  @Override
  public Stream<Path> findJavaFiles(Path root) throws IOException {
    //noinspection resource
    return Files.walk(root)
        .filter(path -> Files.isRegularFile(path) && pathMatcher.matches(path));
  }
}
