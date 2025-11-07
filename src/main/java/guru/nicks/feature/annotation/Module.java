package guru.nicks.feature.annotation;

import org.togglz.core.annotation.FeatureAttribute;
import org.togglz.core.annotation.Owner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom feature attribute describing in which Maven module the feature resides. This is not the same as the built-in
 * {@link Owner @Owner} attribute which may refer to a person.
 */
@FeatureAttribute("Module")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Module {

    /**
     * Maven module: auth-service.
     */
    String AUTH_SERVICE = "auth-service";

    /**
     * Maven module: job-executor-starter.
     */
    String JOB_EXECUTOR_STARTER = "job-executor-starter";

    /**
     * Maven module: feign-starter.
     */
    String FEIGN_STARTER = "feign-starter";

    /**
     * Maven module: jwt-auth-server-starter.
     */
    String JWT_AUTH_SERVER_STARTER = "jwt-auth-server-starter";

    /**
     * Maven module: jwt-resource-server-starter.
     */
    String JWT_RESOURCE_SERVER_STARTER = "jwt-resource-server-starter";

    /**
     * Maven module: main-service.
     */
    String MAIN_SERVICE = "main-service";

    /**
     * Maven module: minimal-app-starter.
     */
    String MINIMAL_APP_STARTER = "minimal-app-starter";

    /**
     * Maven module: mongo-starter.
     */
    String MONGO_STARTER = "mongo-starter";

    /**
     * Maven module: notification-starter.
     */
    String NOTIFICATION_STARTER = "notification-starter";

    /**
     * Maven module: rate-limit-starter.
     */
    String RATE_LIMIT_STARTER = "rate-limit-starter";

    /**
     * Maven module: s3-starter.
     */
    String S3_STARTER = "s3-starter";

    /**
     * Maven module: ses-starter.
     */
    String SES_STARTER = "ses-starter";

    /**
     * Maven module: web-socket-relayer-service.
     */
    String WEB_SOCKET_RELAYER_SERVICE = "web-socket-relayer-service";

    String value();

}
