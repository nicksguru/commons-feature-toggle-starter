package guru.nicks.commons.feature;

import guru.nicks.commons.feature.exception.FeatureDisabledException;

import org.togglz.core.Feature;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Convenient predicate for checking if a {@link Feature} is currently active.
 * <p>
 * The feature states <b>must not be cached</b> because they can be toggled in runtime manually and also may depend on
 * various conditions, such as current date, user's IP address, etc.
 */
public interface FeatureTester extends Predicate<Feature> {

    /**
     * Checks if this feature is currently active and throws an exception if it is not.
     *
     * @param feature           project feature
     * @param exceptionSupplier supplier that provides the exception
     * @throws RuntimeException or its subclass if the feature is not active
     */
    default void checkState(Feature feature, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!test(feature)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Calls {@link #checkState(Feature, Supplier)}, passing a supplier which calls
     * {@link FeatureDisabledException#FeatureDisabledException(Feature)}. This is the most frequent use case.
     *
     * @param feature project feature
     * @throws FeatureDisabledException if the feature is not active
     */
    default void checkState(Feature feature) {
        checkState(feature, () -> new FeatureDisabledException(feature));
    }

}
