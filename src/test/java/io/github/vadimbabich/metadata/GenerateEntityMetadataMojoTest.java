package io.github.vadimbabich.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.github.vadimbabich.metadata.api.JavaLanguageLevel;
import io.github.vadimbabich.metadata.test.matchers.HasStaticFields;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GenerateEntityMetadataMojoTest {

  // Real instance rather than a mock: Byte Buddy cannot instrument MavenProject on JDK 25.
  MavenProject mavenProject;

  @Mock
  Log log;

  @TempDir
  Path tempDir;

  private GenerateEntityMetadataMojo mojo;


  @BeforeEach
  void setUp() {
    mojo = new GenerateEntityMetadataMojo(){
      @Override
      public Log getLog() {
        return log;
      }
    };

    mavenProject = new MavenProject();
    // setFile derives basedir from the pom's parent directory; the pom need not exist on disk
    mavenProject.setFile(
        Paths.get("./src/test/resources/projects/simple-project/pom.xml").toFile());

    mojo.project = mavenProject;
    mojo.languageLevel = JavaLanguageLevel.JAVA_17;
    mojo.sourceDirectory = Path.of("src/main/java");
    mojo.outputDirectory = tempDir.toFile();
    mojo.entityMetadataGenerator = "r2dbc";
  }


  @Test
  void givenValidConfiguration_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.readme";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));

    File userFile = tempDir.resolve("com/example/readme/User_.java").toFile();
    Assertions.assertTrue(userFile.exists(), "Expected User_.java to be generated");
    assertThat(userFile).is(new HasStaticFields(List.of("ID", "NAME")));

    File userAttributeFile = tempDir.resolve("com/example/readme/UserAttribute_.java").toFile();
    Assertions.assertTrue(userAttributeFile.exists(),
        "Expected UserAttribute_.java to be generated");
    assertThat(userAttributeFile)
        .is(new HasStaticFields(List.of("ATTRIBUTE_ID", "USER_ID", "VALUE")));

    assertThat(mavenProject.getCompileSourceRoots())
        .as("generated sources registered as a compile source root")
        .contains(tempDir.toFile().getAbsolutePath());
  }

  @Test
  void givenNullProjectBaseDir_whenExecute_thenThrowsMojoExecutionException() {
    // A project with no pom file has no base directory.
    mojo.project = new MavenProject();

    MojoExecutionException ex = assertThrows(
        MojoExecutionException.class,
        mojo::execute,
        "Expected exception when base directory is null"
    );

    assertTrue(ex.getMessage().contains("Error generating metadata"));
  }

  @Test
  void givenMissingOutputDirectory_whenExecute_thenThrowsMojoExecutionException() {
    mojo.outputDirectory = null;

    MojoExecutionException ex = assertThrows(
        MojoExecutionException.class,
        mojo::execute,
        "Expected exception for missing outputDirectory"
    );

    assertTrue(ex.getMessage().contains("Error generating metadata"));
  }

  @Test
  void givenMyEntityClass_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.entities";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/entities/MyEntity_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected MyEntity_.java to be generated");

    assertThat(expectedFile).is(new HasStaticFields(List.of("ID", "NAME")));
  }

  @Test
  void givenEntityWithNestedClassClass_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.nested";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/nested/EntityWithNestedClass_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected EntityWithNestedClass_.java to be generated");

    assertThat(expectedFile).is(new HasStaticFields(List.of("ID", "VALUE")));
  }

  @Test
  void givenEntityWithNestedRecordClass_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.nested";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/nested/EntityWithNestedRecord_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected EntityWithNestedRecord_.java to be generated");

    assertThat(expectedFile).is(new HasStaticFields(List.of("KEY", "FIELD")));
  }

  @Test
  void givenRecordEntityClass_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.entities";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/entities/RecordEntity_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected RecordEntity_.java to be generated");

    assertThat(expectedFile).is(new HasStaticFields(List.of("ID", "NAME")));
  }

  @Test
  void givenBaseEntityWithoutTableAnnotation_whenExecute_thenNoMetadataClassIsGenerated()
      throws Exception {
    mojo.packageName = "com.example.inherited";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/inherited/BaseEntity_.java").toFile();
    Assertions.assertFalse(expectedFile.exists(),
        "BaseEntity has no @Table annotation, so BaseEntity_.java must not be generated");
  }

  @Test
  void givenSubEntityClass_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.inherited";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/inherited/SubEntity_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected SubEntity_.java to be generated");

    // Declaration order, own fields first, then up the inheritance chain
    assertThat(expectedFile).is(new HasStaticFields(List.of("SUB_FIELD", "MIDDLE_FIELD", "ID")));
  }

  @Test
  void givenNestedClassesAndStaticFields_whenExecute_thenFieldsCollectedRecursively() throws Exception {
    mojo.packageName = "com.example.inherited";
    mojo.languageLevel = JavaLanguageLevel.JAVA_21;

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/inherited/ComplexStructure_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected ComplexStructure_.java to be generated");

    assertThat(expectedFile).is(new HasStaticFields(List.of("TOP_LEVEL", "LEVEL1_FIELD", "LEVEL2_FIELD", "RECORD_FIELD")));
  }

  @Test
  void givenEntityWithCollectionClass_whenExecute_thenMetadataIsGeneratedSuccessfully() throws Exception {
    mojo.packageName = "com.example.entities";

    mojo.execute();

    verify(log, atLeastOnce()).info(contains("Generated metadata"));
    File expectedFile = tempDir.resolve("com/example/entities/EntityWithCollection_.java").toFile();
    Assertions.assertTrue(expectedFile.exists(), "Expected EntityWithCollection_.java to be generated");

    assertThat(expectedFile).is(new HasStaticFields(List.of("ID")));
  }
}
