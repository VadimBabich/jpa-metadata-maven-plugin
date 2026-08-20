package ref;

import io.github.vadimbabich.spike.SpikeReferences;
import org.springframework.data.relational.core.mapping.Table;

// Cross-entity happy path: the referencing side. Emission must record RefTarget's @Id property
// name and type, read through the annotation — the exact cross-entity read that makes isolating
// registration hazardous.
@Table("ref_entity")
@SpikeReferences(target = RefTarget.class)
public record RefEntity(Long id) {

}
