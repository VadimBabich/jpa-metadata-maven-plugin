package bs1;

import org.springframework.data.relational.core.mapping.Table;

// BS-1: one component type is unresolvable. The probe records what the processor observes
// (TypeKind.ERROR) and whether it is invoked at all.
@Table("bs1")
public record Bs1Entity(MissingType missing, String name) {

}
