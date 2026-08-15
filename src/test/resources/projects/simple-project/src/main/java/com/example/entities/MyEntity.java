package com.example.entities;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table( "my_entity")
public class MyEntity {

  @Column("id")
  private Long id;

  @Column("name")
  private String name;

}