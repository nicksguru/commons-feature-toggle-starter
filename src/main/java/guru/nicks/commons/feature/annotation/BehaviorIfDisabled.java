package guru.nicks.commons.feature.annotation;

import org.togglz.core.annotation.FeatureAttribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom feature attribute providing information about the behavior of the application when a feature is disabled.
 */
@FeatureAttribute("If disabled")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BehaviorIfDisabled {

    String STUBBED_WITH_HTTP_404 = "stubbed with HTTP 404";

    /**
     * Description of how the disabled feature behaves.
     *
     * @see #STUBBED_WITH_HTTP_404
     */
    String value();

}
