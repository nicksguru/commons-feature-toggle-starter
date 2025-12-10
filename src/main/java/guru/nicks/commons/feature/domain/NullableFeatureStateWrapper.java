package guru.nicks.commons.feature.domain;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.togglz.core.repository.FeatureState;

import java.io.Serializable;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class NullableFeatureStateWrapper implements Serializable {

    @Nullable
    FeatureState featureState;

}
