package io.github.vadimbabich.metadata.parser.io;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.stream.Stream;

public class FileSystemJavaFileFinder implements JavaFileFinder {

  private final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.java");

  @Override
  public Stream<Path> findJavaFiles(Path root) throws IOException {
    //noinspection resource
    return Files.walk(root)
        .filter(path -> Files.isRegularFile(path) && pathMatcher.matches(path));
  }
}
