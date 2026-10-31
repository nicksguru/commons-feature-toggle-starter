package guru.nicks.commons.feature.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.Feature;

import java.util.function.Predicate;

/**
 * Provides feature-related beans when Togglz is disabled, so that their consumers (for example: internal alerting,
 * which checks a feature to decide whether to send an alert or just log) keep working. All features are reported as
 * disabled.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "togglz", name = "enabled", havingValue = "false")
public class CommonsTogglzStubAutoConfiguration {

    @Bean
    public Predicate<Feature> featureTester() {
        return feature -> false;
    }

}
