package com.example.aggregates;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Embedded-value witness (semantic-coverage matrix, Q5 row). The billing address uses the
 * documented full form with a prefix — Spring prepends it to every column of the embedded class,
 * which a name-per-field model cannot address. The shipping address deliberately carries BOTH
 * {@code @Embedded.Nullable} and {@code @Column}: the presence-only filter is expected to include
 * it even though embedding has no single column.
 */
@Table("invoices")
public record Invoice(
    @Id
    @Column("invoice_id")
    Long id,

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "billing_")
    BillingAddress billingAddress,

    @Embedded.Nullable
    @Column("shipping")
    BillingAddress shippingAddress,

    @Column("total_amount")
    BigDecimal totalAmount
) {

}
