package io.github.vadimbabich.entitymetamodel.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

/**
 * Pins the retention and targets the marker ADRs argue for. Neither marker has an in-repo
 * application yet, so nothing else would notice a switch to SOURCE retention.
 */
class MarkerAnnotationContractTest {

  @Test
  void generatedIsClassRetainedAndTypeTargeted() {
    Retention retention = Generated.class.getAnnotation(Retention.class);
    Target target = Generated.class.getAnnotation(Target.class);

    assertThat(retention.value()).isEqualTo(RetentionPolicy.CLASS);
    assertThat(target.value()).containsExactly(ElementType.TYPE);
  }

  @Test
  void generatedCarriesTheGeneratorIdentifier() throws Exception {
    Class<?> valueType = Generated.class.getDeclaredMethod("value").getReturnType();

    assertThat(valueType).isEqualTo(String[].class);
  }

  @Test
  void rawSqlIsClassRetainedAndMarksMethodsAndTypes() {
    Retention retention = RawSql.class.getAnnotation(Retention.class);
    Target target = RawSql.class.getAnnotation(Target.class);

    assertThat(retention.value()).isEqualTo(RetentionPolicy.CLASS);
    assertThat(target.value()).containsExactlyInAnyOrder(ElementType.METHOD, ElementType.TYPE);
  }
}
