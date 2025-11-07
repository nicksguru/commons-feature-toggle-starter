package guru.nicks.feature.impl;

import guru.nicks.feature.FeatureTester;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;

@RequiredArgsConstructor
public class FeatureTesterImpl implements FeatureTester {

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final FeatureManager featureManager;

    @Override
    public boolean test(Feature feature) {
        return featureManager.isActive(feature);
    }

}
