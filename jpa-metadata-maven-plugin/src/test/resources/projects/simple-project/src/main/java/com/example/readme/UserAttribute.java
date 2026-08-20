package com.example.readme;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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
