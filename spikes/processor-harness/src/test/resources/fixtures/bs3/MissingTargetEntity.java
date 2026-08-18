package bs3;

import io.github.vadimbabich.spike.SpikeReferences;
import org.springframework.data.relational.core.mapping.Table;

// BS-3, case 1: the reference target type does not exist. javac reports the unresolvable symbol;
// the probe asks whether the processor additionally sees the annotation value as an ERROR type
// and can raise its own diagnostic.
@Table("bs3_missing_target")
@SpikeReferences(target = DoesNotExist.class)
public record MissingTargetEntity(Long id) {

}
