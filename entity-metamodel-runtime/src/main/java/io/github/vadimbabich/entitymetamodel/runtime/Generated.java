package io.github.vadimbabich.entitymetamodel.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a generated metamodel class. CLASS retention keeps the marker visible to bytecode tooling
 * such as JaCoCo; there are deliberately no date or comment elements.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Generated {

  /** The generator's identifier, mirroring the JDK annotation's {@code value} contract. */
  String[] value();
}
