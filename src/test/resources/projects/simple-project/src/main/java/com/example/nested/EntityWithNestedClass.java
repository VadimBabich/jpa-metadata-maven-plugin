package com.example.nested;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table( "entity_with_nested_class")
public class EntityWithNestedClass {

  @Column("id")
  private Long id;

  private Nested nested;

  @Table( "nested_entity")
  public static class Nested {
    @Column("value")
    private String value;
  }
}