package com.example.typing;

import java.time.LocalDateTime;
import org.springframework.data.relational.core.mapping.Column;

/** Plain record with typed components, referenced as a field type — no {@code @Table}. */
public record AuditStamp(
    @Column("changed_at")
    LocalDateTime changedAt,

    @Column("changed_by")
    String changedBy
) {

}
