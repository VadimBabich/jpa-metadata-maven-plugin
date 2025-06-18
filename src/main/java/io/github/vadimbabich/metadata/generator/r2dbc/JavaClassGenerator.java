package io.github.vadimbabich.metadata.generator.r2dbc;

import java.io.IOException;

/**
 * Represents a code generation contract for producing Java source classes at build time.
 * <p>
 * It extends {@link ClassNameAware}, allowing consumers to retrieve the fully qualified
 * {@link com.squareup.javapoet.ClassName} of the class being generated.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 *  ClassGenerator generator = new ColumnClassGenerator(...);
 *  generator.generateSourceFile();
 *  ClassName generated = generator.className();
 * </pre>
 *
 * @see ClassNameAware
 * @see com.squareup.javapoet.ClassName
 *
 * @author Vadim Babich
 */
public interface JavaClassGenerator extends ClassNameAware {

  /**
   * Triggers generation of a Java class source file, using {@code JavaPoet}.
   *
   * @throws IOException if the file cannot be written to the output directory
   */
  void generateSourceFile() throws IOException;
}
