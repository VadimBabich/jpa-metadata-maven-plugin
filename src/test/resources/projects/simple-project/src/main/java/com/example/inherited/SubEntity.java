package com.example.inherited;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table( "sub_entity")
public class SubEntity extends MiddleEntity {

  @Column("sub_field")
  private String subField;
}