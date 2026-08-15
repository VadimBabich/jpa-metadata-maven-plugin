package io.github.vadimbabich.metadata.generator.r2dbc;

import io.github.vadimbabich.metadata.api.EntityMetadataGenerator;
import io.github.vadimbabich.metadata.api.EntityMetadataGeneratorFactory;
import io.github.vadimbabich.metadata.api.GeneratedClassNamingStrategy;
import java.io.File;
import org.apache.maven.plugin.logging.Log;

/**
 * The generator backend shipped with the plugin, selected as {@code r2dbc}.
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
