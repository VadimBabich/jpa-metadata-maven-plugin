package com.example.inherited;

import org.springframework.data.relational.core.mapping.Column;

public abstract class BaseEntity{
  @Column("id")
  private Long id;
}