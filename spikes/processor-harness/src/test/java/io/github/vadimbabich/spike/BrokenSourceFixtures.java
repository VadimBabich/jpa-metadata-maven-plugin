package io.github.vadimbabich.spike;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

/** Resolves the probe's fixture sources from the test classpath. */
final class BrokenSourceFixtures {

  private BrokenSourceFixtures() {
  }

  static Path fixture(String relativePath) {
    URL resource = BrokenSourceFixtures.class.getClassLoader()
        .getResource("fixtures/" + relativePath);

    if (resource == null) {
      throw new IllegalStateException("Fixture not on the test classpath: " + relativePath);
    }

    try {
      return Path.of(resource.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Fixture URL is not a file: " + resource, e);
    }
  }
}
