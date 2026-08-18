package com.example.entities;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table( "record_entity")
public record RecordEntity(
    @Column("id") Long id,
    @Column("name") String name
) {}