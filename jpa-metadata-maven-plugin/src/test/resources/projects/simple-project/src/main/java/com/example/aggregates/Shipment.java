package com.example.aggregates;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;
import org.springframework.data.relational.core.mapping.Sequence;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Transient and lifecycle witness — a class, not a record, matching the production corpus shape.
 * The decisive case is {@code draftNote}: Spring excludes any {@code @Transient} property from the
 * mapping context, so a generated constant for it throws lazily at first use; the presence-only
 * {@code @Column} filter is expected to include it anyway.
 */
@Table("shipments")
public class Shipment {

  @Id
  @Sequence(value = "shipment_seq", schema = "logistics")
  @Column("shipment_id")
  Long id;

  @Column("carrier")
  String carrier;

  @Transient
  String cachedLabel;

  @Transient
  @Column("draft_note")
  String draftNote;

  @Version
  @Column("version")
  Long version;

  @InsertOnlyProperty
  @Column("created_at")
  OffsetDateTime createdAt;
}
