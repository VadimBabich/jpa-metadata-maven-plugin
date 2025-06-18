package io.github.vadimbabich.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.vadimbabich.metadata.api.EntityMetadataGenerator;
import io.github.vadimbabich.metadata.api.GeneratedClassNamingStrategy;
import java.io.File;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MetadataGeneratorFactoryTest {

  private Log log;
  private File outputDir;
  private GeneratedClassNamingStrategy classSuffix;


  @BeforeEach
  void setUp() {
    log = Mockito.mock(Log.class);
    outputDir = new File("target/test-output");
    classSuffix = name -> name + "_";
  }

  @Test
  void givenKnownGeneratorName_whenResolve_thenReturnsExpectedGenerator() {
    // Given
    String generatorType = "r2dbc";
    MetadataGeneratorFactory factory = new MetadataGeneratorFactory(generatorType, outputDir, log,
        classSuffix);

    // When
    EntityMetadataGenerator generator = factory.resolve();

    // Then
    assertNotNull(generator, "Expected generator to be non-null for known type");
    assertEquals(
        "io.github.vadimbabich.metadata.generator.r2dbc.R2dbcEntityMetadataGenerator",
        generator.getClass().getName(),
        "Expected correct class for R2DBC generator"
    );
  }

  @Test
  void givenUnknownGeneratorName_whenResolve_thenThrowsIllegalArgumentException() {
    // Given
    String generatorType = "unknown";
    MetadataGeneratorFactory factory = new MetadataGeneratorFactory(generatorType, outputDir, log,
        classSuffix);

    // When / Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        factory::resolve,
        "Expected an IllegalArgumentException for unknown generator type"
    );

    assertTrue(
        exception.getMessage().toLowerCase().contains("unknown"),
        "Exception message should mention the unknown generator type"
    );
  }
}