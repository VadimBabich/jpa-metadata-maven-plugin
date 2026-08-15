package io.github.vadimbabich.metadata.generator.r2dbc;

import com.squareup.javapoet.ClassName;

/**
 * Exposes the name of the class an implementation generates, so generators can reference each
 * other's output without hardcoding names.
 *
 * @author Vadim Babich
 */
public interface ClassNameAware {

  ClassName className();
}
