package guru.nicks.feature.annotation;

import org.togglz.core.annotation.FeatureGroup;
import org.togglz.core.annotation.Label;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A feature may belong to multiple groups.
 */
public @interface ProjectFeatureGroup {

    @FeatureGroup(AccessControl.TITLE)    // seen by custom REST API
    @Label(AccessControl.TITLE)           // seen by Togglz Console
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface AccessControl {

        String TITLE = "Access Control";

    }

    @FeatureGroup(ExclusiveChoices.TITLE) // seen by custom REST API
    @Label(ExclusiveChoices.TITLE)        // seen by Togglz Console
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ExclusiveChoices {

        String TITLE = "Exclusive Choices";

    }

    @FeatureGroup(InternalAlerts.TITLE)   // seen by custom REST API
    @Label(InternalAlerts.TITLE)          // seen by Togglz Console
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface InternalAlerts {

        String TITLE = "Internal Alerts";

    }

    @FeatureGroup(ExternalRestApi.TITLE)  // seen by custom REST API
    @Label(ExternalRestApi.TITLE)         // seen by Togglz Console
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ExternalRestApi {

        String TITLE = "REST API / External";

    }

    @FeatureGroup(InternalRestApi.TITLE)  // seen by custom REST API
    @Label(InternalRestApi.TITLE)         // seen by Togglz Console
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface InternalRestApi {

        String TITLE = "REST API / Internal";

    }

    @FeatureGroup(Unstable.TITLE)         // seen by custom REST API
    @Label(Unstable.TITLE)                // seen by Togglz Console
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Unstable {

        String TITLE = "Unstable Features";

    }

}
