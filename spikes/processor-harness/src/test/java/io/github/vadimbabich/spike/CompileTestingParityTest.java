package io.github.vadimbabich.spike;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Round 2, harness 1: the identical parity assertion expressed through compile-testing 0.23.0.
 *
 * <p>Deliberately not {@code hasSourceEquivalentTo}. That assertion compares ASTs and would stay
 * green while the header comment, the file encoding or the blank lines between constants drifted —
 * which is exactly the drift a shape freeze has to catch.
 */
class CompileTestingParityTest {

  private static final String SOURCE_OUTPUT = "/SOURCE_OUTPUT/";

  private GoldenCorpus corpus;
  private Compilation compilation;

  @BeforeEach
  void compileGoldenEntities() {
    corpus = GoldenCorpus.locate();

    compilation = Compiler.javac()
        .withProcessors(new SpikeMetamodelProcessor())
        .withOptions(
            "--release", "17",
            "-encoding", "UTF-8",
            "-classpath", System.getProperty("java.class.path"))
        .compile(sourceObjects());
  }

  @Test
  void compilesTheGoldenEntitiesWithoutErrors() {
    assertThat(compilation.errors()).isEmpty();
  }

  @Test
  void reproducesTheGoldenCorpusByteForByte() {
    MetamodelParity.against(corpus).assertMatches(generatedSources());
  }

  private List<JavaFileObject> sourceObjects() {
    List<Path> sources = new ArrayList<>(corpus.entitySources());
    sources.addAll(corpus.supportSources());

    List<JavaFileObject> sourceObjects = new ArrayList<>();
    for (Path source : sources) {
      sourceObjects.add(asJavaFileObject(source));
    }

    return sourceObjects;
  }

  private JavaFileObject asJavaFileObject(Path source) {
    try {
      return JavaFileObjects.forResource(source.toUri().toURL());
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Cannot address corpus file " + source, e);
    }
  }

  private Map<String, byte[]> generatedSources() {
    Map<String, byte[]> byRelativePath = new TreeMap<>();

    for (JavaFileObject generated : compilation.generatedSourceFiles()) {
      byRelativePath.put(relativePathOf(generated), readBytes(generated));
    }

    return byRelativePath;
  }

  private String relativePathOf(JavaFileObject generated) {
    String name = generated.getName();
    int sourceRoot = name.indexOf(SOURCE_OUTPUT);

    if (sourceRoot < 0) {
      throw new IllegalStateException("Generated file is not under " + SOURCE_OUTPUT + ": " + name);
    }

    return name.substring(sourceRoot + SOURCE_OUTPUT.length());
  }

  private byte[] readBytes(JavaFileObject generated) {
    try (InputStream contents = generated.openInputStream()) {
      return contents.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read generated file " + generated.getName(), e);
    }
  }
}
