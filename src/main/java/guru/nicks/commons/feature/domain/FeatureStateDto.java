package guru.nicks.commons.feature.domain;

import lombok.Builder;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link FeatureState} cannot be directly deserialized from JSON because it lacks a no-args constructor. This DTO
 * stores all feature state information and provides conversion methods.
 */
@Builder(toBuilder = true)
public record FeatureStateDto(

        String featureName,
        boolean enabled,
        String strategyId,
        Map<String, String> parameters) implements Serializable {

    /**
     * Creates DTO from {@link FeatureState}.
     *
     * @param featureState feature state
     * @return DTO
     */
    public static FeatureStateDto from(FeatureState featureState) {
        Map<String, String> parameters = featureState.getParameterNames()
                .stream()
                .collect(Collectors.toMap(
                        name -> name,
                        featureState::getParameter
                ));

        return FeatureStateDto.builder()
                .featureName(featureState.getFeature().name())
                .enabled(featureState.isEnabled())
                .strategyId(featureState.getStrategyId())
                .parameters(parameters)
                .build();
    }

    /**
     * Converts DTO back to {@link FeatureState}.
     *
     * @param feature feature to associate with this state
     * @return feature state
     */
    public FeatureState toFeatureState(Feature feature) {
        var featureState = new FeatureState(feature, enabled);
        featureState.setStrategyId(strategyId);

        if (parameters != null) {
            parameters.forEach(featureState::setParameter);
        }

        return featureState;
    }

}
