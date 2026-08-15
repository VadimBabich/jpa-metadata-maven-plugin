package io.github.vadimbabich.metadata.test.matchers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.assertj.core.api.Condition;

/**
 * Asserts that a Java source file declares exactly these public static final fields, in this
 * order. The order is part of the assertion because generated output has to be deterministic.
 */
public class HasStaticFields extends Condition<File> {

  private final List<String> expectedFields;

  public HasStaticFields(List<String> expectedFields) {
    super("file with exactly these static field constants, in order: " + expectedFields);
    this.expectedFields = expectedFields;
  }

  @Override
  public boolean matches(File file) {
    CompilationUnit cu;
    try {
      cu = StaticJavaParser.parse(file);
    } catch (IOException e) {
      // An unreadable file is a broken test, not a field mismatch. Fail loudly rather than
      // reporting a misleading "fields differ".
      throw new UncheckedIOException("Cannot read generated file: " + file, e);
    }

    List<String> actualFields = cu.findAll(FieldDeclaration.class).stream()
        .filter(f -> f.isPublic() && f.isStatic() && f.isFinal())
        .flatMap(f -> f.getVariables().stream())
        .map(NodeWithSimpleName::getNameAsString)
        .toList();

    return actualFields.equals(expectedFields);
  }
}
