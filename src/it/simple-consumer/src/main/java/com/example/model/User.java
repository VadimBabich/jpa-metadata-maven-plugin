package com.example.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * The README's worked example. The integration test generates and compiles its metamodel, then
 * diffs it against the golden corpus.
 */
@Immutable
@Table("users")
public record User(
    @Id
    @Column("user_id")
    String id,

    @Column("user_name")
    String name
) {

}
