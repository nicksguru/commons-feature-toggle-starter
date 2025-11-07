package guru.nicks.feature.config;

import guru.nicks.feature.FeatureTester;
import guru.nicks.feature.impl.FeatureTesterImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.togglz.core.Feature;
import org.togglz.core.manager.CompositeFeatureProvider;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.spi.FeatureProvider;

/**
 * Works if {@code togglz.enabled} is true.
 * <p>
 * Configures {@link FeatureProvider} for Togglz to borrow features from the given enum only. If
 * {@code togglz.feature-enums} is used without this fix, Togglz <b>adds</b> features found in enums to those specified
 * in properties - even if they have the same name, so there will be, surprisingly, <b>two</b> features with the same
 * name. Therefore, {@link CompositeFeatureProvider} is replaced with {@link EnumBasedFeatureProvider} here.
 *
 * @see FeatureTester
 */
@ConditionalOnProperty(prefix = "togglz", name = "enabled", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class TogglzAutoConfiguration {

    @ConditionalOnMissingBean(FeatureTester.class)
    @Bean
    public FeatureTester featureTester(FeatureManager featureManager) {
        return new FeatureTesterImpl(featureManager);
    }

    /**
     * Add Togglz web console endpoint to the list printed by {@code /actuator}, to it can be clicked (not only typed).
     */
    @Endpoint(id = "togglz-console")
    @Component
    public static class TogglzConsoleEndpoint {

        /**
         * There must be at least one method annotated with e.g. {@link ReadOperation} (no matter what it does),
         * otherwise the endpoint will not be registered.
         */
        @ReadOperation
        public String dummy() {
            return "dummy";
        }

    }

    /**
     * Needs a separate {@link Configuration @Configuration} to avoid cyclic bean dependencies (outer class needs
     * {@link FeatureManager} created by this inner class).
     */
    @Configuration(proxyBeanMethods = false)
    static class Beans {

        @Value("${togglz.feature-enums}")
        private Class<? extends Feature> featuresEnumClass;

        @Bean
        public FeatureProvider featureProvider() {
            if (!featuresEnumClass.isEnum()) {
                throw new IllegalStateException("Configured class " + featuresEnumClass.getName() + " is not an enum");
            }

            return new EnumBasedFeatureProvider(featuresEnumClass);
        }

    }

}
