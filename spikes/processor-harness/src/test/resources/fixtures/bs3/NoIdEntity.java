package bs3;

import io.github.vadimbabich.spike.SpikeReferences;
import org.springframework.data.relational.core.mapping.Table;

// BS-3, case 2: the target resolves but has no @Id property. The processor must reject this with
// a Messager ERROR anchored on this element; nothing may be emitted for it.
@Table("bs3_no_id")
@SpikeReferences(target = NoIdTarget.class)
public record NoIdEntity(Long id) {

}
