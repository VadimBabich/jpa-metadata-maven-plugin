package com.example.entities;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.util.List;

@Table( "entity_with_collection")
public class EntityWithCollection {

  @Column("id")
  private Long id;

  private List<String> tags;

  private String[] codes;
}