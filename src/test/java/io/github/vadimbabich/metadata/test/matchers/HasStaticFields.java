package io.github.vadimbabich.metadata.test.matchers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import java.io.File;
import java.util.List;
import org.assertj.core.api.Condition;

/**
 * AssertJ {@link Condition} that verifies whether a given Java source file contains the specified
 * static final field names.
 * <p>
 * Parses the file as a Java compilation unit and checks for public static final fields matching the
 * expected names.
 */
public class HasStaticFields extends Condition<File> {

  private final List<String> expectedFields;

  public HasStaticFields(List<String> expectedFields) {
    super("file with static field constants: " + expectedFields);
    this.expectedFields = expectedFields;
  }

  @Override
  public boolean matches(File file) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(file);
      List<String> actualFields = cu.findAll(FieldDeclaration.class).stream()
          .filter(f -> f.isPublic() && f.isStatic() && f.isFinal())
          .flatMap(f -> f.getVariables().stream())
          .map(NodeWithSimpleName::getNameAsString)
          .toList();

      return actualFields.containsAll(expectedFields);
    } catch (Exception e) {
      return false;
    }
  }
}
