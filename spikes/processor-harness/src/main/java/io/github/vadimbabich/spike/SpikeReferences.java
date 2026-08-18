package io.github.vadimbabich.spike;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spike-local cross-entity marker. Deliberately <em>not</em> named {@code @References}: the
 * relationship vocabulary — name, retention, coordinates — is an open WS-6 decision this throwaway
 * asset must not pre-empt.
 *
 * <p>Retention is load-bearing for the staleness experiment: CLASS retention puts the target's
 * class literal into the annotated entity's constant pool, which Gradle's class-dependency
 * analysis can see; SOURCE retention removes that edge. Round 4 measures both.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface SpikeReferences {

  Class<?> target();
}
