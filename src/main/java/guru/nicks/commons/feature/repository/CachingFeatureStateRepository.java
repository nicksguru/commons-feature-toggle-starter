package guru.nicks.commons.feature.repository;

import guru.nicks.commons.feature.domain.NullableFeatureStateWrapper;

import am.ik.yavi.meta.ConstraintArguments;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Caches feature states. What's being cached is {@link NullableFeatureStateWrapper}, not the state itself, because
 * {@code null} state is returned from the underlying repository if it has no information - and this state of things
 * must be cached. This is a normal situation which makes {@link FeatureManager} check if the feature is enabled by
 * default i.e. annotated with {@link EnabledByDefault @EnabledByDefault}.
 * <p>
 * Most cache engines (Redis, Caffeine) do not support nulls, therefore they're stored as
 * {@link NullableFeatureStateWrapper}.
 */
@Slf4j
public class CachingFeatureStateRepository implements StateRepository {

    private final StateRepository delegate;
    private final Function<Feature, NullableFeatureStateWrapper> cacheGetter;
    private final BiConsumer<Feature, NullableFeatureStateWrapper> cacheUpdater;

    /**
     * Constructor.
     *
     * @param delegate     underlying state repository to delegate to when the cache is missed
     * @param cacheGetter  supplier that retrieves the feature state from the cache, must return {@code null} for a
     *                     cache miss
     * @param cacheUpdater consumer that updates the cache with a new feature state (see note above on nulls)
     */
    @ConstraintArguments
    public CachingFeatureStateRepository(StateRepository delegate,
            Function<Feature, NullableFeatureStateWrapper> cacheGetter,
            BiConsumer<Feature, NullableFeatureStateWrapper> cacheUpdater) {
        this.delegate = checkNotNull(delegate, _CachingFeatureStateRepositoryArgumentsMeta.DELEGATE.name());

        this.cacheGetter = checkNotNull(cacheGetter,
                _CachingFeatureStateRepositoryArgumentsMeta.CACHEGETTER.name());
        this.cacheUpdater = checkNotNull(cacheUpdater,
                _CachingFeatureStateRepositoryArgumentsMeta.CACHEUPDATER.name());
    }

    @Nullable
    @Override
    public FeatureState getFeatureState(Feature feature) {
        NullableFeatureStateWrapper wrapper = cacheGetter.apply(feature);
        // null means key not found in cache; empty Optional means null is cached
        if (wrapper != null) {
            return wrapper.getFeatureState();
        }

        // can be null
        FeatureState featureState = delegate.getFeatureState(feature);
        updateCache(feature, featureState, "get");
        return featureState;
    }

    @Override
    public void setFeatureState(FeatureState featureState) {
        delegate.setFeatureState(featureState);

        try {
            updateCache(featureState.getFeature(), featureState, "update");
        }
        // log exception to make cache update failure stand out (the cache keeps holding the old state!)
        catch (RuntimeException e) {
            log.error("Failed to update cache for feature '{}'", featureState.getFeature().name(), e);
            throw e;
        }
    }

    /**
     * Updates the cache with the given feature state (wrapped in {@link NullableFeatureStateWrapper}).
     *
     * @param feature      feature whose state is being updated in the cache
     * @param featureState feature state, can be {@code null}
     * @param reason       the reason for the cache update (e.g., "get", "update"), used for logging only
     */
    private void updateCache(Feature feature, @Nullable FeatureState featureState, String reason) {
        log.debug("Caching feature state upon {}: {}={}", reason, feature.name(),
                (featureState == null)
                        ? "null (for real value, see feature's @EnabledByDefault)"
                        : (featureState.isEnabled() ? "enabled" : "disabled"));

        var wrapper = (featureState == null)
                // save memory - use immutable singleton
                ? NullableFeatureStateWrapper.EMPTY
                : NullableFeatureStateWrapper.builder()
                        .featureState(featureState)
                        .build();
        cacheUpdater.accept(feature, wrapper);
    }

}
