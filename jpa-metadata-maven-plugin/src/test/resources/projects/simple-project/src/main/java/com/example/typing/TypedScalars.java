package com.example.typing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Scalar typing witnesses for the typing-fidelity study: primitives (the boxing rule),
 * a boxed-vs-primitive pair on one entity, enum, temporals, BigDecimal — and two acronym-bearing
 * property names, because the golden corpus exercises neither pass of the constant-name split
 * {@code htmlURL} hits the lowercase→uppercase pass only; {@code sourceURLPath}
 * is the only witness of the acronym-boundary pass.
 */
@Table("typed_scalars")
public record TypedScalars(
    @Id
    @Column("id")
    long id,

    @Column("retry_count")
    int retryCount,

    @Column("active")
    boolean active,

    @Column("boxed_retry_count")
    Integer boxedRetryCount,

    @Column("status")
    Status status,

    @Column("created_at")
    LocalDateTime createdAt,

    @Column("synced_at")
    Instant syncedAt,

    @Column("total_price")
    BigDecimal totalPrice,

    @Column("html_url")
    String htmlURL,

    @Column("source_url_path")
    String sourceURLPath
) {

}
