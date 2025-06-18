package io.github.vadimbabich.metadata.api;

import java.io.File;
import org.apache.maven.plugin.logging.Log;

/**
 * A factory interface for creating instances of {@link EntityMetadataGenerator}.
 * <p>
 * Implementations of this interface are intended to be discovered via the Java
 * {@link java.util.ServiceLoader} mechanism.
 * </p>
 *
 * @author Vadim Babich
 */
public interface EntityMetadataGeneratorFactory {

  /**
   * Returns the unique name identifying this factory implementation.
   *
   * @return the name of the generator type (e.g., "r2dbc", "jpa", "default")
   */
  String name();

  /**
   * Creates a new instance of {@link EntityMetadataGenerator} with the provided context
   * parameters.
   *
   * @param classNamingStrategy the strategy to use for naming generated classes
   * @param outputDir           the directory where generated sources should be written
   * @param log                 the Maven plugin log for debug and status output
   * @return a fully initialized {@link EntityMetadataGenerator} instance
   */
  EntityMetadataGenerator create(GeneratedClassNamingStrategy classNamingStrategy,
      File outputDir, Log log);
}
