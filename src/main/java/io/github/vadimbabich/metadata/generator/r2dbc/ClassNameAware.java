package io.github.vadimbabich.metadata.generator.r2dbc;

import com.squareup.javapoet.ClassName;

/**
 * Provides access to the {@link ClassName} representing a specific class, used in code generation
 * scenarios.
 *
 * <p>Implementations are expected to return the fully qualified name generated class.
 *
 * @author Vadim Babich
 */
public interface ClassNameAware {

  /**
   * Returns the {@link ClassName} of the target or generated class.
   *
   * @return the {@code ClassName} instance representing the class.
   */
  ClassName className();
}
