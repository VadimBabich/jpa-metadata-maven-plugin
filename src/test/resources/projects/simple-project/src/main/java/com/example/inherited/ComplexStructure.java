package com.example.inherited;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("complex_structure")
public class ComplexStructure {

  @Column("top_level")
  private String topLevel;

  public static final String STATIC_FIELD = "STATIC";

  @Table("nested_level_1")
  public static class NestedLevel1 {

    @Column("level1_field")
    private String level1Field;

    @Table("nested_level_2")
    public static class NestedLevel2 {

      @Column("level2_field")
      private String level2Field;

      public static final String LEVEL_2_STATIC = "LEVEL2";
    }
  }

  /**
   * Java 21 style record inside entity.
   */
  @Table("embedded_record")
  public record EmbeddedRecord(@Column("record_field") String recordField) {}
}