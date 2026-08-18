package com.example.nested;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table( "entity_with_nested_record")
public class EntityWithNestedRecord {

  @Column("key")
  private String key;

  private NestedRecord nested;

  @Table( "nested_record_entity")
  public record NestedRecord(
      @Column("field") String field
  ) {}
}