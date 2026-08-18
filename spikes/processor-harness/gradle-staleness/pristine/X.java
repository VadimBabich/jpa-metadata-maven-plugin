package fixture;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("x")
public class X {

  @Id
  Long id;

  String name;
}
