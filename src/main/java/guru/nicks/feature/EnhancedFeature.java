package guru.nicks.feature;

import guru.nicks.feature.annotation.BehaviorIfDisabled;
import guru.nicks.feature.annotation.HowToToggle;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.togglz.core.Feature;
import org.togglz.core.annotation.FeatureGroup;
import org.togglz.core.annotation.Label;
import org.togglz.core.annotation.Owner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface EnhancedFeature extends Feature {

    /**
     * Always throws exception because this method should never be called.
     *
     * @throws IllegalStateException on each call
     */
    @Override
    default boolean isActive() {
        throw new IllegalStateException("Please use "
                + FeatureTester.class.getSimpleName()
                + " bean instead of this method");
    }

    /**
     * Reads {@link HowToToggle#toggleableOnline()}, if any, falling back to {@code true} because the missing annotation
     * means the feature is toggleable online.
     *
     * @return online togglability
     */
    default boolean toggleableOnline() {
        return findAnnotationValue(HowToToggle.class, HowToToggle::toggleableOnline)
                .orElse(true);
    }

    /**
     * Collects all non-blank {@link FeatureGroup#value()}'s.
     *
     * @return group titles (the list may be empty, but never {@code null})
     */
    default List<String> getGroupNames() {
        return findAnnotations(FeatureGroup.class, true).stream()
                .map(FeatureGroup::value)
                .filter(StringUtils::isNotBlank)
                // List is more handy for callers than TreeSet (has indexes)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Reads {@link Label#value()}.
     *
     * @return feature label
     * @throws IllegalStateException label missing or blank
     */
    default String getLabel() {
        // find NON-merged annotations only because @FeatureGroup has a @Label too
        String label = findAnnotations(Label.class, false).stream()
                .map(Label::value)
                .filter(StringUtils::isNotBlank)
                // coalesce duplicate values
                .distinct()
                .findFirst()
                .orElse(null);

        if (StringUtils.isBlank(label)) {
            throw new IllegalStateException(
                    "Missing non-blank @" + Label.class.getSimpleName() + " for feature " + name());
        }

        return label;
    }

    /**
     * Reads non-blank {@link BehaviorIfDisabled#value()}}, if any.
     *
     * @return feature behavior in disabled state
     */
    default Optional<String> findBehaviorIfDisabled() {
        return findAnnotationValue(BehaviorIfDisabled.class, BehaviorIfDisabled::value);
    }

    /**
     * Reads non-blank {@link HowToToggle#value()}}, if any.
     *
     * @return description of how to toggle the feature
     */
    default Optional<String> findHowToToggle() {
        return findAnnotationValue(HowToToggle.class, HowToToggle::value);
    }

    /**
     * Reads non-blank {@link Owner#value()}}, if any.
     *
     * @return in which Maven module the feature resides
     */
    default Optional<String> findOwner() {
        return findAnnotationValue(Owner.class, Owner::value);
    }

    /**
     * Finds all annotations of the given type for this enum member.
     *
     * @param annotationClass annotation class
     * @param findMerged      find merged annotations (if not, only those explicitly set on the field are found)
     * @param <T>             annotation type
     * @return annotation instance or empty if not found
     */
    default <T extends Annotation> Set<T> findAnnotations(Class<T> annotationClass, boolean findMerged) {
        Field field;

        try {
            field = getClass().getField(name());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to get enum field '" + name() + "': " + e.getMessage(), e);
        }

        return findMerged
                ? AnnotatedElementUtils.findAllMergedAnnotations(field, annotationClass)
                : Arrays.stream(field.getAnnotationsByType(annotationClass))
                        .collect(Collectors.toSet());
    }

    /**
     * Extracts a single non-empty (as per {@link Object#toString()}) merged annotation value.
     *
     * @param annotationClass the annotation class to search for
     * @param mapper          function to extract the desired value from the annotation
     * @param <A>             annotation type
     * @param <R>             return type
     * @return optional extracted value
     * @throws IllegalStateException multiple annotations of the same type are found
     */
    default <A extends Annotation, R> Optional<R> findAnnotationValue(Class<A> annotationClass, Function<A, R> mapper) {
        // coalesce duplicate values
        Set<R> items = findAnnotations(annotationClass, true).stream()
                .map(mapper)
                .filter(result -> StringUtils.isNotBlank(Objects.toString(result, null)))
                .collect(Collectors.toSet());

        return switch (items.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(items.iterator().next());
            default -> throw new IllegalStateException(
                    "Multiple @" + annotationClass.getSimpleName() + " found for feature " + name());
        };
    }

}
