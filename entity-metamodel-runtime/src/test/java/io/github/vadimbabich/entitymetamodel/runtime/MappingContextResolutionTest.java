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
 * Resolution delegates to Spring's mapping context — the standing keeper: SQL names are never
 * re-implemented, and no name is ever baked into a ref. These are the runtime-module forms of the
 * lifecycle note's LT-S1/LT-S2: with no component and no static, "context isolation" reduces to
 * plain values answering only for the context they were handed.
 */
class MappingContextResolutionTest {

  @Test
  void columnOverrideAndStrategyFallbackBothResolveThroughTheContext() {
    RelationalMappingContext mappingContext = new RelationalMappingContext();
    EntityRef<Account> account = EntityRef.of(Account.class);

    // @Column("account_id") wins over any strategy; ownerEmail falls back to the default
    // snake_case strategy — both answers are the context's, not ours.
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

    // One ref, two contexts: each context's answer is its own — there is no shared state to
    // contaminate (the demo proved the 1.x static holder fails exactly this).
    assertThat(ownerEmail.columnName(defaultNaming)).isEqualTo("owner_email");
    assertThat(ownerEmail.columnName(shoutingNaming)).isEqualTo("OWNEREMAIL");
    assertThat(ownerEmail.columnName(defaultNaming)).isEqualTo("owner_email");
  }

  @Test
  void unknownPropertyFailsFastWithEntityAndPropertyInTheMessage() {
    RelationalMappingContext mappingContext = new RelationalMappingContext();
    PropertyRef<Account, String> bogus =
        EntityRef.of(Account.class).property("nope", String.class);

    // The 1.x shape threw lazily at first dereference deep inside rendering; the runtime fails
    // at the resolution call with a diagnosable message (lifecycle note S4's spirit).
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bogus.columnName(mappingContext))
        .withMessageContaining("nope")
        .withMessageContaining("Account");
  }

  @Test
  void nameIsTheCompileTimeAnswerAndNeedsNoContext() {
    // PropertyRef.name() is the nameOf(...) drop-in — the 187-site migration path.
    assertThat(EntityRef.of(Account.class).property("ownerEmail", String.class).name())
        .isEqualTo("ownerEmail");
  }
}
