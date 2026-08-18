package ref;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

// Cross-entity happy path: the referenced side. A class, not a record, so the parity path never
// emits a metamodel for it — mirroring the Gradle staleness fixture exactly.
@Table("ref_target")
public class RefTarget {

  @Id
  Long id;

  String name;
}
