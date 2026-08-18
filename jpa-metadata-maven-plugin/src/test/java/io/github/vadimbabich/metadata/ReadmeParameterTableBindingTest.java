package io.github.vadimbabich.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Binds the README "Parameters" table to the generated plugin descriptor (binding map row B5):
 * a parameter added, renamed, re-defaulted, or re-flagged without a README row update must fail
 * the build. Bound level: names, defaults, and effective requiredness (required AND no default —
 * the README column documents what a user must configure, not the raw descriptor flag). Prose
 * descriptions stay unbound. Read-only parameters ({@code project}) are excluded: the README
 * documents user parameters only.
 */
class ReadmeParameterTableBindingTest {

  private static final String FIX_HINT =
      " — update README 'Parameters' and this binding together (binding map row B5)";

  private record DocumentedParameter(String name, boolean requiredMark, String defaultValue) {
  }

  private record DescriptorParameter(String name, boolean requiredFlag, String defaultValue) {
  }

  private static Map<String, DocumentedParameter> documentedParameters;
  private static Map<String, DescriptorParameter> descriptorParameters;

  @BeforeAll
  static void loadBothSurfaces() throws Exception {
    documentedParameters = parseReadmeParametersTable();
    descriptorParameters = parseGenerateMetadataDescriptor();
  }

  @Test
  void readmeDocumentsExactlyTheUserParameters() {
    assertThat(documentedParameters.keySet())
        .as("README parameter rows vs descriptor user parameters" + FIX_HINT)
        .containsExactlyInAnyOrderElementsOf(descriptorParameters.keySet());
  }

  @Test
  void requiredMarksMatchEffectiveRequiredness() {
    documentedParameters.forEach((name, documented) -> {
      DescriptorParameter descriptor = descriptorParameters.get(name);

      boolean effectivelyRequired = descriptor.requiredFlag() && descriptor.defaultValue() == null;
      assertThat(documented.requiredMark())
          .as("Effective requiredness of '%s' (required AND no default)%s", name, FIX_HINT)
          .isEqualTo(effectivelyRequired);
    });
  }

  @Test
  void defaultValuesMatchTheDescriptor() {
    documentedParameters.forEach((name, documented) -> {
      DescriptorParameter descriptor = descriptorParameters.get(name);

      assertThat(normalizedDefault(documented.defaultValue()))
          .as("Default value of '%s'%s", name, FIX_HINT)
          .isEqualTo(normalizedDefault(descriptor.defaultValue()));
    });
  }

  /**
   * Path defaults are compared trailing-slash-insensitively; "none" means no default.
   */
  private static String normalizedDefault(String defaultValue) {
    if (defaultValue == null || defaultValue.isEmpty() || defaultValue.equals("none")) {
      return "";
    }

    if (defaultValue.endsWith("/")) {
      return defaultValue.substring(0, defaultValue.length() - 1);
    }
    return defaultValue;
  }

  private static Map<String, DocumentedParameter> parseReadmeParametersTable() throws IOException {
    List<String> tableRows = readmeParameterTableRows();

    Map<String, DocumentedParameter> parameters = new LinkedHashMap<>();
    for (String row : tableRows) {
      String[] columns = row.split("\\|");
      String name = columns[1].trim();
      boolean requiredMark = columns[2].contains("✅");
      String defaultValue = columns[3].trim();

      parameters.put(name, new DocumentedParameter(name, requiredMark, defaultValue));
    }

    assertThat(parameters).as("README 'Parameters' table rows").isNotEmpty();
    return parameters;
  }

  private static List<String> readmeParameterTableRows() throws IOException {
    Path readme = Paths.get("..", "README.md").toAbsolutePath().normalize();
    assertThat(readme).as("repository-root README next to this module").exists();

    List<String> rows = new ArrayList<>();
    boolean insideParametersSection = false;
    for (String line : Files.readAllLines(readme)) {
      if (line.startsWith("## ")) {
        insideParametersSection = line.equals("## Parameters");
        continue;
      }

      boolean isDataRow = line.startsWith("|") && !line.startsWith("| Parameter")
          && !line.startsWith("|--") && !line.startsWith("|-");
      if (insideParametersSection && isDataRow) {
        rows.add(line);
      }
    }
    return rows;
  }

  private static Map<String, DescriptorParameter> parseGenerateMetadataDescriptor()
      throws Exception {
    Element mojo = generateMetadataMojoElement();

    Map<String, String> defaults = new LinkedHashMap<>();
    Element configuration = (Element) mojo.getElementsByTagName("configuration").item(0);
    NodeList configurationEntries = configuration.getChildNodes();
    for (int i = 0; i < configurationEntries.getLength(); i++) {
      if (configurationEntries.item(i) instanceof Element entry) {
        defaults.put(entry.getTagName(), entry.getAttribute("default-value"));
      }
    }

    Map<String, DescriptorParameter> parameters = new LinkedHashMap<>();
    NodeList parameterElements = mojo.getElementsByTagName("parameter");
    for (int i = 0; i < parameterElements.getLength(); i++) {
      Element parameter = (Element) parameterElements.item(i);
      String name = textOf(parameter, "name");
      boolean editable = Boolean.parseBoolean(textOf(parameter, "editable"));
      if (!editable) {
        continue;
      }

      boolean requiredFlag = Boolean.parseBoolean(textOf(parameter, "required"));
      String defaultValue = defaults.get(name);
      if (defaultValue != null && defaultValue.isEmpty()) {
        defaultValue = null;
      }
      parameters.put(name, new DescriptorParameter(name, requiredFlag, defaultValue));
    }

    assertThat(parameters).as("editable parameters of goal generate-metadata").isNotEmpty();
    return parameters;
  }

  private static Element generateMetadataMojoElement() throws Exception {
    try (InputStream descriptor = ReadmeParameterTableBindingTest.class
        .getResourceAsStream("/META-INF/maven/plugin.xml")) {
      assertThat(descriptor).as("generated plugin descriptor on the test classpath").isNotNull();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Document document = factory.newDocumentBuilder().parse(descriptor);

      NodeList mojos = document.getElementsByTagName("mojo");
      for (int i = 0; i < mojos.getLength(); i++) {
        Element mojo = (Element) mojos.item(i);
        if ("generate-metadata".equals(textOf(mojo, "goal"))) {
          return mojo;
        }
      }
    }
    throw new AssertionError("goal 'generate-metadata' not found in the plugin descriptor");
  }

  private static String textOf(Element parent, String childTag) {
    NodeList children = parent.getElementsByTagName(childTag);
    return children.getLength() == 0 ? "" : children.item(0).getTextContent();
  }
}
