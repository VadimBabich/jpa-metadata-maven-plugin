package bs3;

import org.springframework.data.relational.core.mapping.Table;

// BS-3, case 2 target: resolvable, but carries no @Id property. Deliberately a class, not a
// record — the parity emission path only fires for records, so this type never gets a metamodel
// of its own.
@Table("no_id_target")
public class NoIdTarget {

  String name;
}
