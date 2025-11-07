package guru.nicks.feature.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FeatureStability {

    ALPHA,
    BETA,
    RC,
    STABLE;

    private final String stringValue;

    FeatureStability() {
        stringValue = name().toLowerCase();
    }

    @JsonCreator
    public static FeatureStability ofStringValue(String stringValue) {
        return valueOf(stringValue);
    }

    /**
     * Accompanied by {@link #ofStringValue(String)}, defines JSON serialization and deserialization of this enum using
     * {@link #getStringValue()}.
     */
    @JsonValue
    @Override
    public String toString() {
        return stringValue;
    }

}
