package guru.nicks.commons.feature.repository;

import guru.nicks.commons.feature.domain.NullableFeatureStateWrapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.togglz.core.Feature;
import org.togglz.core.repository.StateRepository;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;

/**
 * Caches feature states in Redis.
 *
 * @see #of(StateRepository, RedisTemplate, Function, Duration)
 */
@Slf4j
public class RedisCachingFeatureStateRepository extends CachingFeatureStateRepository {

    private RedisCachingFeatureStateRepository(StateRepository delegate,
            Function<Feature, NullableFeatureStateWrapper> cacheGetter,
            BiConsumer<Feature, NullableFeatureStateWrapper> cacheUpdater) {
        super(delegate, cacheGetter, cacheUpdater);
    }

    /**
     * Creates a new instance of {@link RedisCachingFeatureStateRepository}.
     *
     * @param delegate        underlying state repository to delegate to when the cache is missed
     * @param redisTemplate   Redis template for cache operations
     * @param cacheKeyBuilder function that builds cache keys for features
     * @param cacheTtl        TTL for the cache entries
     * @return new instance
     */
    public static RedisCachingFeatureStateRepository of(StateRepository delegate,
            RedisTemplate<String, Object> redisTemplate, Function<Feature, String> cacheKeyBuilder, Duration cacheTtl) {
        check(cacheTtl, "feature state cache TTL")
                .notNull()
                .constraint(duration -> !duration.isNegative(), "must not be negative");

        Function<Feature, NullableFeatureStateWrapper> cacheGetter = feature -> {
            try {
                String key = cacheKeyBuilder.apply(feature);
                return (NullableFeatureStateWrapper) redisTemplate.opsForValue().get(key);
            } catch (RuntimeException e) {
                log.error("Redis read failed, falling back to database lookup: {}", e.getMessage(), e);
                // this will trigger a database lookup
                return null;
            }
        };

        BiConsumer<Feature, NullableFeatureStateWrapper> cacheUpdater = (feature, wrapper) -> {
            String key = cacheKeyBuilder.apply(feature);
            redisTemplate.opsForValue().set(key, wrapper, cacheTtl);
        };

        return new RedisCachingFeatureStateRepository(delegate, cacheGetter, cacheUpdater);
    }

}
