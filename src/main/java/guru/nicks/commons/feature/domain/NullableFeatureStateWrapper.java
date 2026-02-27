package guru.nicks.commons.feature.domain;

import guru.nicks.commons.feature.repository.CachingFeatureStateRepository;

import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.io.Serializable;

/**
 * Wrapper for {@link FeatureState} that can hold {@code null} values.
 * <p>
 * Uses {@link FeatureStateDto} for JSON serialization since {@link FeatureState} cannot be directly deserialized by
 * Jackson.
 *
 * @see CachingFeatureStateRepository
 */
public record NullableFeatureStateWrapper(
        FeatureStateDto featureStateDto) implements Serializable {

    public static final NullableFeatureStateWrapper EMPTY = new NullableFeatureStateWrapper(null);

    /**
     * Creates a wrapper from a {@link FeatureState}.
     *
     * @param featureState the feature state to wrap, may be {@code null}
     * @return wrapper containing the DTO representation
     */
    public static NullableFeatureStateWrapper of(FeatureState featureState) {
        return (featureState == null)
                ? EMPTY
                : new NullableFeatureStateWrapper(FeatureStateDto.from(featureState));
    }

    /**
     * Converts the wrapper back to a {@link FeatureState}.
     *
     * @param feature the feature enum to associate with the state
     * @return the feature state, or {@code null} if this wrapper is empty
     */
    public FeatureState toFeatureState(Feature feature) {
        return (featureStateDto == null)
                ? null
                : featureStateDto.toFeatureState(feature);
    }

}
