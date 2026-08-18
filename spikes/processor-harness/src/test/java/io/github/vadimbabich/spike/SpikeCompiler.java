package io.github.vadimbabich.spike;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Runs one annotation-processing round through {@code javax.tools} and nothing else.
 *
 * <p>This is the zero-dependency baseline the harness comparison is measured against: it cannot
 * lose the JDK-tolerance criterion, because it ships with the JDK. Keeping the compile invocation
 * behind one seam is the point — if a chosen sugar harness later breaks on a new JDK, only this
 * class is replaced.
 */
final class SpikeCompiler {

  private final Path workDir;

  SpikeCompiler(Path workDir) {
    this.workDir = workDir;
  }

  CompilationOutcome compile(List<Path> sources, Processor processor) {
    try {
      return runCompilation(sources, processor);
    } catch (IOException e) {
      throw new UncheckedIOException("Compilation round could not be run", e);
    }
  }

  private CompilationOutcome runCompilation(List<Path> sources, Processor processor)
      throws IOException {

    Path classOutput = Files.createDirectories(workDir.resolve("classes"));
    Path generatedSourceRoot = Files.createDirectories(workDir.resolve("generated-sources"));

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    boolean succeeded;

    try (StandardJavaFileManager fileManager =
        compiler.getStandardFileManager(diagnostics, null, UTF_8)) {

      JavaCompiler.CompilationTask task = compiler.getTask(
          null,
          fileManager,
          diagnostics,
          compilerOptions(classOutput, generatedSourceRoot),
          null,
          fileManager.getJavaFileObjectsFromPaths(sources));

      task.setProcessors(List.of(processor));
      succeeded = task.call();
    }

    return new CompilationOutcome(
        succeeded,
        collectMessages(diagnostics),
        readGeneratedSources(generatedSourceRoot));
  }

  private List<String> compilerOptions(Path classOutput, Path generatedSourceRoot) {
    // --release 17 pins the language level across every JDK in the matrix, so a failing cell fails
    // because of the JDK, not because javac defaulted to a newer source level.
    return List.of(
        "--release", "17",
        "-encoding", "UTF-8",
        "-classpath", System.getProperty("java.class.path"),
        "-d", classOutput.toString(),
        "-s", generatedSourceRoot.toString());
  }

  private List<CompilationOutcome.Message> collectMessages(
      DiagnosticCollector<JavaFileObject> diagnostics) {

    List<CompilationOutcome.Message> messages = new ArrayList<>();

    for (var diagnostic : diagnostics.getDiagnostics()) {
      messages.add(new CompilationOutcome.Message(diagnostic.getKind(), diagnostic.toString()));
    }

    return List.copyOf(messages);
  }

  private Map<String, byte[]> readGeneratedSources(Path generatedSourceRoot) throws IOException {
    try (Stream<Path> generated = Files.walk(generatedSourceRoot)) {
      Map<String, byte[]> byRelativePath = new TreeMap<>();

      generated.filter(Files::isRegularFile)
          .forEach(file -> byRelativePath.put(
              generatedSourceRoot.relativize(file).toString(),
              readBytes(file)));

      return Map.copyOf(byRelativePath);
    }
  }

  private byte[] readBytes(Path file) {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read generated source " + file, e);
    }
  }
}
