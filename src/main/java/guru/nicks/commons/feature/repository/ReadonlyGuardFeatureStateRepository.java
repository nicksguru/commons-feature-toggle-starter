package guru.nicks.commons.feature.repository;

import guru.nicks.commons.feature.EnhancedFeature;

import jakarta.annotation.Nullable;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;

/**
 * Forbids editing features whose {@link EnhancedFeature#toggleableOnline()} is {@code false}. Must be the FIRST
 * repository in the chain.
 */
public class ReadonlyGuardFeatureStateRepository implements StateRepository {

    /**
     * Always returns {@code null}, so Togglz tries the next repository in the chain.
     */
    @Nullable
    @Override
    public FeatureState getFeatureState(Feature feature) {
        return null;
    }

    /**
     * @throws IllegalStateException if trying to set a feature state that is not toggleable online (as per
     *                               {@link EnhancedFeature#toggleableOnline()})
     */
    @Override
    public void setFeatureState(FeatureState featureState) {
        String featureName = featureState.getFeature().name();
        var feature = featureState.getFeature();

        if (feature instanceof EnhancedFeature enhancedFeature
                && !enhancedFeature.toggleableOnline()) {
            throw new IllegalStateException("Feature is not toggleable online: '" + featureName + "'");
        }
    }

}
