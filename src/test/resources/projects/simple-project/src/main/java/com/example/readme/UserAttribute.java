package io.github.vadimbabich.metadata;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Immutable
@Table("user_attributes")
public record UserAttribute(
    @Id
    @Column("uaat_id")
    Long attributeId,

    @Column("uaat_user_id")
    String userId,

    @Column("uaat_value")
    String value
) {

}
