package io.github.vadimbabich.metadata;


import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Immutable
@Table("users")
public record User(
    @Id
    @Column("user_id")
    String id,

    @Column("user_name")
    String name
) {

}