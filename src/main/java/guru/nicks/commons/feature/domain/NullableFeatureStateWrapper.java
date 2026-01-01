package guru.nicks.commons.feature.domain;

import jakarta.annotation.Nullable;
import lombok.Builder;
import org.togglz.core.repository.FeatureState;

@Builder(toBuilder = true)
public record NullableFeatureStateWrapper(

        @Nullable
        FeatureState featureState) {
}
