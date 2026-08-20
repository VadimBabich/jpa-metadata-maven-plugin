package io.github.vadimbabich.entitymetamodel.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The D3 invariant: no static mutable state in this module — no non-final statics, and final
 * statics only of known-immutable types.
 */
class NoStaticMutableStateTest {

  private static final List<Class<?>> IMMUTABLE_STATIC_TYPES =
      List.of(String.class, Class.class);

  @Test
  void noClassInTheModuleDeclaresStaticMutableState() throws Exception {
    List<String> violations = new ArrayList<>();

    for (Class<?> moduleClass : allModuleClasses()) {
      for (Field field : moduleClass.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          continue;
        }

        if (!Modifier.isFinal(field.getModifiers())) {
          violations.add(moduleClass.getName() + "." + field.getName() + " is a non-final static");
          continue;
        }

        boolean immutable = field.getType().isPrimitive()
            || IMMUTABLE_STATIC_TYPES.contains(field.getType());
        if (!immutable) {
          violations.add(moduleClass.getName() + "." + field.getName()
              + " is a final static of mutable-capable type " + field.getType().getName());
        }
      }
    }

    assertThat(violations).isEmpty();
  }

  private List<Class<?>> allModuleClasses() throws URISyntaxException {
    Path classesRoot = Path.of(
        EntityRef.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    try (Stream<Path> classFiles = Files.walk(classesRoot)) {
      List<Class<?>> moduleClasses = new ArrayList<>();

      classFiles
          .filter(file -> file.toString().endsWith(".class"))
          .forEach(file -> moduleClasses.add(loadClass(classesRoot, file)));

      assertThat(moduleClasses).isNotEmpty();
      return moduleClasses;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot walk the module's classes", e);
    }
  }

  private Class<?> loadClass(Path classesRoot, Path classFile) {
    String binaryName = classesRoot.relativize(classFile).toString()
        .replace(java.io.File.separatorChar, '.')
        .replaceAll("\\.class$", "");

    try {
      return Class.forName(binaryName, false, EntityRef.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Module class not loadable: " + binaryName, e);
    }
  }
}
