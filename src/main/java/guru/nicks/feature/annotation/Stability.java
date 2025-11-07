package guru.nicks.feature.annotation;

import guru.nicks.feature.domain.FeatureStability;

import org.togglz.core.annotation.FeatureAttribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom feature attribute describing the feature stability status. For stable features, this attribute can be omitted
 * to reduce visual noise.
 */
@FeatureAttribute("Stability")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Stability {

    FeatureStability value();

}
