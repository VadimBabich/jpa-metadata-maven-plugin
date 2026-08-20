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
@Table("user_attributes")
public record UserAttribute(
    @Id
    @Column("usat_id")
    Long attributeId,

    @Column("usat_user_id")
    String userId,

    @Column("usat_value")
    String attributeValue
) {

}
