package com.example.aggregates;

import org.springframework.data.relational.core.mapping.Column;

/**
 * Plain embedded value class — no {@code @Table}. The pipeline must not generate a metamodel for
 * it; its columns exist only prefixed inside the owning entity's table.
 */
public record BillingAddress(
    @Column("street")
    String street,

    @Column("city")
    String city
) {

}
