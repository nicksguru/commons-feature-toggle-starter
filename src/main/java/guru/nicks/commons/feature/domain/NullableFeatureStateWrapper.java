package guru.nicks.commons.feature.domain;

import guru.nicks.commons.feature.repository.CachingFeatureStateRepository;

import lombok.Builder;
import org.togglz.core.repository.FeatureState;

import java.io.Serializable;

/**
 * Wrapper for {@link FeatureState} that can hold {@code null} values.
 *
 * @see CachingFeatureStateRepository
 */
@Builder(toBuilder = true)
public record NullableFeatureStateWrapper(
        FeatureState featureState) implements Serializable {

    public static final NullableFeatureStateWrapper EMPTY = NullableFeatureStateWrapper.builder().build();

}
