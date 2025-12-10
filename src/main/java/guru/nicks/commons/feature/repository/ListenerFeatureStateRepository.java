package guru.nicks.commons.feature.repository;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;

/**
 * Notifies about feature state changes. Must be the LAST repository in the chain.
 */
@Slf4j
public class ListenerFeatureStateRepository implements StateRepository {

    @Nullable
    @Override
    public FeatureState getFeatureState(Feature feature) {
        return null;
    }

    @Override
    public void setFeatureState(FeatureState featureState) {
        log.info("Feature state updated: {}={}", featureState.getFeature().name(),
                featureState.isEnabled() ? "enabled" : "disabled");
    }

}
