package io.github.vadimbabich.entitymetamodel.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vadimbabich.entitymetamodel.runtime.fixtures.Account;
import io.github.vadimbabich.entitymetamodel.runtime.fixtures.Membership;
import org.junit.jupiter.api.Test;

/**
 * JoinRef contract (ws6-joinref-contract-extension): pure data over a (source, target) property
 * pair sharing one value type — FK-type compatibility is a compile-time property of generated
 * code, not a runtime check. Instance-parametric traversal (the owner's N-instance requirement)
 * composes from re-anchoring, so a JoinRef names a relationship, never an instance.
 */
class JoinRefContractTest {

  private static final EntityRef<Membership> MEMBERSHIP = EntityRef.of(Membership.class);
  private static final EntityRef<Account> ACCOUNT = EntityRef.of(Account.class);

  @Test
  void carriesSourceAndTargetAsPureData() {
    PropertyRef<Membership, Long> accountId = MEMBERSHIP.property("accountId", Long.class);
    PropertyRef<Account, Long> id = ACCOUNT.property("id", Long.class);

    JoinRef<Membership, Account> account = JoinRef.of(accountId, id);

    assertThat(account.source()).isEqualTo(accountId);
    assertThat(account.target()).isEqualTo(id);
  }

  @Test
  void equalityIsValueIdentityOverThePropertyPair() {
    JoinRef<Membership, Account> first = JoinRef.of(
        MEMBERSHIP.property("accountId", Long.class), ACCOUNT.property("id", Long.class));
    JoinRef<Membership, Account> second = JoinRef.of(
        MEMBERSHIP.property("accountId", Long.class), ACCOUNT.property("id", Long.class));
    JoinRef<Membership, Account> other = JoinRef.of(
        MEMBERSHIP.property("sponsorAccountId", Long.class), ACCOUNT.property("id", Long.class));

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
    assertThat(first).isNotEqualTo(other);
    assertThat(first.equals(second)).isEqualTo(second.equals(first));
  }

  @Test
  void nInstanceTraversalComposesFromReAnchoring() {
    JoinRef<Membership, Account> account = JoinRef.of(
        MEMBERSHIP.property("accountId", Long.class), ACCOUNT.property("id", Long.class));

    // The owner requirement (2026-08-18): one relationship, N target instances. The JoinRef
    // stays instance-free; the builder re-anchors its target per instance — second and third
    // instances are ordinary re-anchored refs, unbounded in count.
    EntityRef<Account> second = ACCOUNT.as("2");
    EntityRef<Account> third = ACCOUNT.as("3");

    assertThat(account.target().of(second)).isEqualTo(second.property("id", Long.class));
    assertThat(account.target().of(third)).isEqualTo(third.property("id", Long.class));
    assertThat(account.target().of(second)).isNotEqualTo(account.target().of(third));
  }
}
