package io.github.vadimbabich.entitymetamodel.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an API entry point that accepts raw SQL fragments. Raw fragments bypass every safety the
 * typed surface provides — callers own quoting, identifier validity and, above all, injection
 * safety: never build the fragment from untrusted input; bind values are still passed as
 * parameters, never concatenated.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RawSql {
}
