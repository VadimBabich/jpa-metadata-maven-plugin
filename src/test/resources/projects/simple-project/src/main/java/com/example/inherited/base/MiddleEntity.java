package com.example.inherited;

import org.springframework.data.relational.core.mapping.Column;


public class MiddleEntity extends BaseEntity {

    @Column("middle_field")
    private String middleField;
}