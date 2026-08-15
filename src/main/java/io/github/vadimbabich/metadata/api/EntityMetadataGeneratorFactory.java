package io.github.vadimbabich.metadata.api;

import java.io.File;
import org.apache.maven.plugin.logging.Log;

/**
 * Creates {@link EntityMetadataGenerator} instances. Implementations are discovered through
 * {@link java.util.ServiceLoader}, so a backend is added by putting its jar on the plugin's
 * classpath.
 *
 * @author Vadim Babich
 */
public interface EntityMetadataGeneratorFactory {

  /**
   * Identifier users select with the {@code entityMetadataGenerator} parameter, for example
   * {@code r2dbc}. Must be unique across the factories on the classpath.
   */
  String name();

  EntityMetadataGenerator create(GeneratedClassNamingStrategy classNamingStrategy,
      File outputDir, Log log);
}
