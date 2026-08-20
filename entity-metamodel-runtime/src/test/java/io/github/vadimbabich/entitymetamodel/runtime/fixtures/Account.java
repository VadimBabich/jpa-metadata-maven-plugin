package io.github.vadimbabich.entitymetamodel.runtime.fixtures;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Resolution fixture: one {@code @Column}-overridden property, one strategy-resolved property —
 * the two name-resolution paths the runtime must delegate to the mapping context (never
 * re-implement, the standing keeper).
 */
@Table("accounts")
public class Account {

  @Id
  @Column("account_id")
  Long id;

  String ownerEmail;
}
