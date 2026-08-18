package io.github.vadimbabich.entitymetamodel.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exact declared-type fidelity (typing study, normative table): generics, deeper generics and
 * arrays carry verbatim — the difference between usable and decorative type parameters.
 */
class TypeRefFidelityTest {

  @Test
  void plainTypeRendersItsQualifiedName() {
    assertThat(TypeRef.of("java.lang.String").canonical()).isEqualTo("java.lang.String");
  }

  @Test
  void genericTypeCarriesItsArguments() {
    TypeRef listOfString = TypeRef.parameterized(
        "java.util.List", List.of(TypeRef.of("java.lang.String")));

    assertThat(listOfString.canonical()).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test
  void deeperGenericsRenderVerbatim() {
    TypeRef mapStringInteger = TypeRef.parameterized(
        "java.util.Map",
        List.of(TypeRef.of("java.lang.String"), TypeRef.of("java.lang.Integer")));

    assertThat(mapStringInteger.canonical())
        .isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
  }

  @Test
  void arrayDimensionsArePreserved() {
    assertThat(TypeRef.array(TypeRef.of("int"), 1).canonical()).isEqualTo("int[]");
    assertThat(TypeRef.array(TypeRef.of("java.lang.String"), 2).canonical())
        .isEqualTo("java.lang.String[][]");
  }

  @Test
  void typeRefsAreValueEqual() {
    TypeRef first = TypeRef.parameterized("java.util.List", List.of(TypeRef.of("java.lang.String")));
    TypeRef second = TypeRef.parameterized("java.util.List", List.of(TypeRef.of("java.lang.String")));

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }
}
