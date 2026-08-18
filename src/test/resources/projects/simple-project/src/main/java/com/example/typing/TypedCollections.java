package com.example.typing;

import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Parameterized-type witnesses: annotated collection, array, deeper generic, bounded wildcard,
 * and a typed-record field — the cases where {@code PropertyRef<E,T>}'s type argument is either
 * faithful or decorative (generic-fidelity requirement, model spec §2).
 */
@Table("typed_collections")
public record TypedCollections(
    @Id
    @Column("id")
    Long id,

    @Column("tags")
    List<String> tags,

    @Column("scores")
    int[] scores,

    @Column("attributes")
    Map<String, Integer> attributes,

    @Column("limits")
    List<? extends Number> limits,

    @Column("audit")
    AuditStamp audit
) {

}
