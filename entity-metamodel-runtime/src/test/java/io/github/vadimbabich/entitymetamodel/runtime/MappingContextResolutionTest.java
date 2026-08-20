package io.github.vadimbabich.entitymetamodel.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.vadimbabich.entitymetamodel.runtime.fixtures.Account;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Resolution delegates to Spring's mapping context: SQL names are never re-implemented and no name
 * is ever baked into a ref. With no component and no static state, context isolation reduces to
 * plain values answering only for the context they were handed.
 */
class MappingContextResolutionTest {

  @Test
  void columnOverrideAndStrategyFallbackBothResolveThroughTheContext() {
    RelationalMappingContext mappingContext = new RelationalMappingContext();
    EntityRef<Account> account = EntityRef.of(Account.class);

    // @Column wins over any strategy; ownerEmail falls back to the default snake_case strategy.
    assertThat(account.property("id", Long.class).columnName(mappingContext))
        .isEqualTo("account_id");
    assertThat(account.property("ownerEmail", String.class).columnName(mappingContext))
        .isEqualTo("owner_email");
    assertThat(account.tableName(mappingContext)).isEqualTo("accounts");
  }

  @Test
  void twoContextsAnswerIndependently_theS1IsolationProperty() {
    RelationalMappingContext defaultNaming = new RelationalMappingContext();
    RelationalMappingContext shoutingNaming = new RelationalMappingContext(new NamingStrategy() {
      @Override
      public String getColumnName(RelationalPersistentProperty property) {
        return property.getName().toUpperCase(Locale.ROOT);
      }
    });

    PropertyRef<Account, String> ownerEmail =
        EntityRef.of(Account.class).property("ownerEmail", String.class);

    // The 1.x static holder failed exactly this: one ref, two contexts, no shared state.
    assertThat(ownerEmail.columnName(defaultNaming)).isEqualTo("owner_email");
    assertThat(ownerEmail.columnName(shoutingNaming)).isEqualTo("OWNEREMAIL");
    assertThat(ownerEmail.columnName(defaultNaming)).isEqualTo("owner_email");
  }

  @Test
  void unknownPropertyFailsFastWithEntityAndPropertyInTheMessage() {
    RelationalMappingContext mappingContext = new RelationalMappingContext();
    PropertyRef<Account, String> bogus =
        EntityRef.of(Account.class).property("nope", String.class);

    // The 1.x shape threw lazily at first dereference, deep inside rendering.
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bogus.columnName(mappingContext))
        .withMessageContaining("nope")
        .withMessageContaining("Account");
  }

  @Test
  void nameIsTheCompileTimeAnswerAndNeedsNoContext() {
    assertThat(EntityRef.of(Account.class).property("ownerEmail", String.class).name())
        .isEqualTo("ownerEmail");
  }
}
