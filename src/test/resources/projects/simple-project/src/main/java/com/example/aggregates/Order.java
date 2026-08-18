package com.example.aggregates;

import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One-to-many witness: the documented {@code @MappedCollection(idColumn = …)} form. The collection
 * lives in the child's table, not as a column here — and the child is a plain record, the
 * documented shape for aggregate members.
 */
@Table("orders")
public record Order(
    @Id
    @Column("order_id")
    Long id,

    @MappedCollection(idColumn = "order_id")
    Set<OrderItem> items,

    @Column("placed_at")
    OffsetDateTime placedAt
) {

}
