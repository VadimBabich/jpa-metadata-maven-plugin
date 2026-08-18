package io.github.vadimbabich.entitymetamodel.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.vadimbabich.entitymetamodel.runtime.fixtures.Account;
import org.junit.jupiter.api.Test;

/**
 * The ratified ref-equality contract (review unit, 2026-08-18): value identity over
 * {@code (entityType, propertyName, alias)} — alias INCLUDED, because distinct aliases are
 * distinct table instances by J2's own semantics. The 1.x {@code Column_}'s context-dependent,
 * asymmetric equals is the recorded anti-pattern these tests keep dead.
 */
class RefEqualityContractTest {

  @Test
  void entityRefsWithSameTypeAndAliasAreEqual() {
    EntityRef<Account> first = EntityRef.of(Account.class);
    EntityRef<Account> second = EntityRef.of(Account.class);

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  void aliasedCopyIsADistinctInstanceWithDistinctIdentity() {
    EntityRef<Account> defaultInstance = EntityRef.of(Account.class);
    EntityRef<Account> secondInstance = defaultInstance.as("2");

    assertThat(secondInstance).isNotEqualTo(defaultInstance);
    assertThat(secondInstance.alias()).isEqualTo("account_2");
    assertThat(defaultInstance.alias()).isEqualTo("account");
  }

  @Test
  void propertyRefEqualityIncludesTheAlias() {
    PropertyRef<Account, Long> onDefault = EntityRef.of(Account.class).property("id", Long.class);
    PropertyRef<Account, Long> onSecond =
        EntityRef.of(Account.class).as("2").property("id", Long.class);
    PropertyRef<Account, Long> onDefaultAgain =
        EntityRef.of(Account.class).property("id", Long.class);

    assertThat(onDefault).isEqualTo(onDefaultAgain);
    assertThat(onDefault.hashCode()).isEqualTo(onDefaultAgain.hashCode());
    assertThat(onDefault).isNotEqualTo(onSecond);
  }

  @Test
  void equalsIsSymmetricAndNullSafeAndTypeSafe() {
    PropertyRef<Account, Long> ref = EntityRef.of(Account.class).property("id", Long.class);
    PropertyRef<Account, Long> same = EntityRef.of(Account.class).property("id", Long.class);

    // The 1.x defect was asymmetry (column_.equals(column) true, column.equals(column_) false).
    assertThat(ref.equals(same)).isEqualTo(same.equals(ref));
    assertThat(ref.equals(null)).isFalse();
    assertThat(ref.equals("id")).isFalse();
  }

  @Test
  void reAnchoringBindsThePropertyToTheOtherInstance() {
    EntityRef<Account> parent = EntityRef.of(Account.class);
    EntityRef<Account> child = parent.as("2");
    PropertyRef<Account, Long> id = parent.property("id", Long.class);

    PropertyRef<Account, Long> reAnchored = id.of(child);

    assertThat(reAnchored.entity()).isEqualTo(child);
    assertThat(reAnchored.name()).isEqualTo("id");
    assertThat(reAnchored).isNotEqualTo(id);
    assertThat(reAnchored).isEqualTo(child.property("id", Long.class));
  }

  @Test
  void theProjectionSeparatorIsReservedInAliases() {
    EntityRef<Account> account = EntityRef.of(Account.class);

    // Layer-2 rule (alias spec): "__" separates <tableAlias>__<column> in projected labels; an
    // alias containing it would corrupt hydration boundaries. Asserted at construction.
    assertThatIllegalArgumentException()
        .isThrownBy(() -> account.as("legacy__copy"))
        .withMessageContaining("__");
  }
}
