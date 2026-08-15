package io.github.vadimbabich.metadata;

import io.github.vadimbabich.metadata.api.EntityMetadataGenerator;
import io.github.vadimbabich.metadata.api.EntityMetadataGeneratorFactory;
import io.github.vadimbabich.metadata.api.GeneratedClassNamingStrategy;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.maven.plugin.logging.Log;

/**
 * Resolves the {@link EntityMetadataGenerator} named by the {@code entityMetadataGenerator}
 * parameter from the factories on the classpath. With no name given, resolution succeeds only when
 * exactly one factory is present.
 *
 * @author Vadim Babich
 */
public class MetadataGeneratorFactory {

  private final Log log;
  private final File outputDir;
  private final String selectedName;
  private final GeneratedClassNamingStrategy classNamingStrategy;
  private final Map<String, EntityMetadataGeneratorFactory> factories = new HashMap<>();

  public MetadataGeneratorFactory(String selectedName, File outputDir, Log log,
      GeneratedClassNamingStrategy classNamingStrategy
  ) {
    this.log = log;
    this.outputDir = outputDir;
    this.selectedName = selectedName;
    this.classNamingStrategy = classNamingStrategy;
    discoverFactories();
  }

  private void discoverFactories() {
    ServiceLoader<EntityMetadataGeneratorFactory> loader =
        ServiceLoader.load(EntityMetadataGeneratorFactory.class);

    for (EntityMetadataGeneratorFactory factory : loader) {
      String name = factory.name();

      log.debug("Discovered generator factory: " + name + " -> " + factory.getClass().getName());

      if (factories.containsKey(name)) {
        throw new IllegalStateException("Duplicate generator factory name: " + name);
      }
      factories.put(name, factory);
    }
  }

  public EntityMetadataGenerator resolve() {
    if (factories.isEmpty()) {
      throw new IllegalStateException("No generator factories found via ServiceLoader.");
    }

    if (selectedName == null || selectedName.isBlank()) {
      if (factories.size() == 1) {
        return factories.values().iterator().next().create(classNamingStrategy, outputDir, log);
      }

      throw new IllegalStateException(
          "Multiple generator factories found. Specify one via plugin property.");
    }

    EntityMetadataGeneratorFactory factory = factories.get(selectedName);
    if (factory == null) {
      throw new IllegalArgumentException("Unknown generator: '" + selectedName +
          "'. Supported: " + getSupportedGeneratorNames());
    }

    return factory.create(classNamingStrategy, outputDir, log);
  }

  public Set<String> getSupportedGeneratorNames() {
    return Collections.unmodifiableSet(factories.keySet());
  }
}