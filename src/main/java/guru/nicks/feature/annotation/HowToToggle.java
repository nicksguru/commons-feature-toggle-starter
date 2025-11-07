package guru.nicks.feature.annotation;

import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.FeatureAttribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom feature attribute describing how to toggle this feature (assuming it can't be done online).
 */
@FeatureAttribute("How to toggle")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HowToToggle {

    String MODULE_REBUILD_REQUIRED = "edit @EnabledByDefault, rebuild module and applications";

    /**
     * Description of how the disabled feature behaves.
     *
     * @see #MODULE_REBUILD_REQUIRED
     */
    String value();

    /**
     *
     * If {@code true}, feature state can be edited in Togglz Console (or directly in DB), and the change comes into
     * effect immediately. Otherwise, editing requires updating {@link EnabledByDefault @EnabledByDefault} and
     * rebuilding all the affected apps - because most likely Spring beans creation is affected.
     * <p>
     * The default is {@code false} because this annotation is commonly used to describe non-togglable features.
     */
    boolean toggleableOnline() default false;

}
