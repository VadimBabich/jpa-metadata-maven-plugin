package io.github.vadimbabich.metadata.generator.r2dbc;

import io.github.vadimbabich.metadata.api.EntityMetadataGenerator;
import io.github.vadimbabich.metadata.api.EntityMetadataGeneratorFactory;
import io.github.vadimbabich.metadata.api.GeneratedClassNamingStrategy;
import java.io.File;
import org.apache.maven.plugin.logging.Log;

/**
 * Factory for creating {@link R2dbcEntityMetadataGenerator} instances.
 * <p>
 * This implementation is registered under the name {@code "r2dbc"} and is intended
 * for generating metadata classes based on Spring Data R2DBC.
 * </p>
 *
 * @author Vadim Babich
 */
public class R2dbcMetadataGeneratorFactory implements EntityMetadataGeneratorFactory {

  @Override
  public String name() {
    return "r2dbc";
  }

  @Override
  public EntityMetadataGenerator create(
      GeneratedClassNamingStrategy classNamingStrategy, File outputDir, Log log) {
    return new R2dbcEntityMetadataGenerator(classNamingStrategy, outputDir, log);
  }
}
