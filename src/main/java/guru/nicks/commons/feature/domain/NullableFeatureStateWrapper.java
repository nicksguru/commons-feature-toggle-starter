package guru.nicks.commons.feature.domain;

import jakarta.annotation.Nullable;
import lombok.Builder;
import org.togglz.core.repository.FeatureState;

import java.io.Serializable;

@Builder(toBuilder = true)
public record NullableFeatureStateWrapper(

        @Nullable
        FeatureState featureState) implements Serializable {
}
