package fixture;

import io.github.vadimbabich.spike.SpikeReferences;
import org.springframework.data.relational.core.mapping.Table;

@Table("e")
@SpikeReferences(target = X.class)
public record E(Long id) {

}
