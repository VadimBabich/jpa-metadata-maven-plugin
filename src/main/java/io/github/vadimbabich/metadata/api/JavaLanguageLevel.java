package io.github.vadimbabich.metadata.api;

/**
 * Supported Java language levels for parsing source code.
 *
 * <p>Used to configure the Java parser to correctly interpret syntax and features
 * available in the specified version.
 */
public enum JavaLanguageLevel {
  JAVA_8, JAVA_11, JAVA_17, JAVA_21
}
