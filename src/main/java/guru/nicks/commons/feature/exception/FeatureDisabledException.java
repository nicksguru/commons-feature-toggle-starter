package guru.nicks.commons.feature.exception;

import guru.nicks.commons.exception.http.NotImplementedException;

import org.togglz.core.Feature;

public class FeatureDisabledException extends NotImplementedException {

    /**
     * Constructor accepting the feature name which is then mentioned in the exception message.
     *
     * @param feature the feature that is disabled
     */
    public FeatureDisabledException(Feature feature) {
        super("Feature disabled: " + feature);
    }

}
