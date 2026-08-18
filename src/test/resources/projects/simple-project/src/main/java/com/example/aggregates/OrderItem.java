package com.example.aggregates;

import org.springframework.data.relational.core.mapping.Column;

/** Aggregate member without {@code @Table} — must not receive a metamodel of its own. */
public record OrderItem(
    @Column("product_code")
    String productCode,

    @Column("quantity")
    int quantity
) {

}
