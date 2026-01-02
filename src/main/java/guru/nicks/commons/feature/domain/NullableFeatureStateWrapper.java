package guru.nicks.commons.feature.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.togglz.core.repository.FeatureState;

import java.io.Serializable;

@Value
@NonFinal
@Builder(toBuilder = true)
public class NullableFeatureStateWrapper implements Serializable {

    public static final NullableFeatureStateWrapper EMPTY = NullableFeatureStateWrapper.builder().build();

    FeatureState featureState;

}
