package guru.nicks.commons.feature;

import guru.nicks.commons.feature.exception.FeatureDisabledException;
import guru.nicks.commons.utils.ReflectionUtils;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.collections.MapUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.togglz.core.Feature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Feature-dependent decorator. Installs interceptor on all methods having non-empty
 * {@link #findRequiredFeature(Class)}. Works both for interface-based and class-based beans, including controllers and
 * beans having no default constructor.
 * <p>
 * <b>WARNING: As of Spring Boot 3.5.8, controllers cannot be wrapped: endpoints returning void (e.g. those having
 * {@code DeleteMapping}) stop being called - no matter if the feature is enabled or not. The reason is unknown. For
 * this reason, this post processor throws an exception when applied to a controller.</b>
 * <p>
 * Methods declared in {@link Object} class are always passed through because, for example,
 * {@link Object#equals(Object)} is needed to keep {@link Map}s functioning. Other public methods of annotated Spring
 * beans, if the specific feature is disabled (as per {@link FeatureTester}) <b>at the moment of the call</b>:
 * <ul>
 *  <li>For controller methods (if user has access, i.e. Spring Security filters go first):
 *      <ul>
 *          <li>{@link FeatureDisabledException} is thrown</li>
 *      </ul>
 *  </li>
 *  <li>For non-controller methods:
 *      <ul>
 *          <li>
 *              If the decorated method returns {@code void}, it isn't called. Semantically it's the same as if the
 *              decorated method checked the feature state in the very first statement of its body and returned if the
 *              feature is disabled.
 *          </li>
 *          <li>
 *              If the decorated method returns non-void, {@link FeatureDisabledException} is thrown because returning
 *              {@code null} may impede the business logic. More complex use cases require manual
 *              {@link FeatureTester#checkState(Feature) handling} in the source code.
 *          </li>
 *      </ul>
 *  </li>
 * </ul>
 * <p>
 */
@RequiredArgsConstructor
@Slf4j
public abstract class FeatureBeanPostProcessor implements BeanPostProcessor {

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final Predicate<Feature> featureTester;

    /**
     * Reads enabler feature from, most commonly, a custom annotation.
     *
     * @param clazz class to check
     * @return feature the class depends on, if any
     */
    public abstract Optional<Feature> findRequiredFeature(Class<?> clazz);

    /**
     * Checks if class is annotated with {@link Controller @Controller} or a derived annotation, such as
     * {@link RestController @RestController}.
     *
     * @param clazz class to check
     * @return {@code true} class is a controller
     */
    public boolean isController(Class<?> clazz) {
        return AnnotatedElementUtils.hasAnnotation(clazz, Controller.class);
    }

    /**
     * Collects, in the whole class hierarchy, public methods returning non-void (excluding {@link Object} because, for
     * example, {@link Object#toString()} always remains available).
     *
     * @param targetClass class to check
     * @return map where the keys are class names and the values are lists of such methods
     */
    public Map<String, List<Method>> findNonVoidPublicMethods(Class<?> targetClass) {
        return ReflectionUtils.getClassHierarchyMethods(targetClass)
                .stream()
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                // isAssignableFrom() not needed for void class
                .filter(method -> method.getReturnType() != void.class)
                .filter(method -> !method.getDeclaringClass().equals(Object.class))
                .sorted(Comparator.comparing(Method::getName))
                .collect(Collectors.groupingBy(method -> method.getDeclaringClass().getName()));
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Feature feature = findRequiredFeature(bean.getClass()).orElse(null);
        if (feature == null) {
            return bean;
        }

        // can't subclass bean.getClass() because it might be a JDK proxy (which is a final class)...
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        // ...but if the real target class is final, it can't be subclassed either
        if (Modifier.isFinal(targetClass.getModifiers())) {
            throw new IllegalArgumentException("Cannot wrap final class [" + targetClass.getName()
                    + "] for feature ["
                    + feature
                    + "]. Consider making the class non-final or using interface-based proxies.");
        }

        boolean targetIsController = isController(targetClass);

        // see class-level comment
        if (targetIsController) {
            throw new IllegalArgumentException("Cannot wrap controllers: endpoints returning void "
                    + "(e.g. those having @DeleteMapping) stop being called no matter if the feature is enabled or "
                    + "not. The reason is unknown.");
        }

        log.info("Making all public methods of bean [{}] dependent on feature '{}'. {}", targetClass.getName(), feature,
                buildExplanationMessage(targetClass, targetIsController));

        var interceptor = MethodCallInterceptor.builder()
                .proxyTarget(bean)
                .proxyTargetIsController(targetIsController)
                .feature(feature)
                .featureTester(featureTester)
                .build();
        Class<?> wrapperClass = generateWrapperClass(targetClass, interceptor);

        // the original class may not have a default constructor (beans having injected dependencies usually do not)
        return ReflectionUtils.instantiateEvenWithoutDefaultConstructor(wrapperClass);
    }

    /**
     * Builds feature behavior explanation message for logging purposes.
     *
     * @param targetClass the target class being wrapped
     * @return explanation message
     */
    private String buildExplanationMessage(Class<?> targetClass, boolean targetIsController) {
        if (targetIsController) {
            return "All endpoints will throw ["
                    + FeatureDisabledException.class.getName()
                    + "] if the feature is disabled";
        }

        StringBuilder explanation = new StringBuilder(
                "A disabled feature causes method calls to be skipped for methods returning void.");

        Map<String, List<Method>> nonVoidPublicMethods = findNonVoidPublicMethods(targetClass);

        if (!MapUtils.isEmpty(nonVoidPublicMethods)) {
            explanation.append(" Found methods return non-void - for them, a disabled feature will throw [")
                    .append(FeatureDisabledException.class.getName())
                    .append("]: ")
                    .append(nonVoidPublicMethods);
        }

        return explanation.toString();
    }

    /**
     * Generates a wrapper class for the given target class.
     *
     * @param targetClass class to wrap
     * @param interceptor method interceptor
     * @return generated wrapper class
     */
    private Class<?> generateWrapperClass(Class<?> targetClass, MethodCallInterceptor interceptor) {
        return new ByteBuddy()
                // more meaningful suffix than default 'ByteBuddy'
                .with(new NamingStrategy.Suffixing(getClass().getSimpleName()))
                .subclass(targetClass)
                // intercept public methods not declared in Object class (equals(), hashCode(), etc.)
                .method(ElementMatchers
                        .isPublic()
                        .and(not(ElementMatchers.isDeclaredBy(Object.class))))
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();
    }

    /**
     * This class MUST be public, otherwise ByteBuddy won't be able to delegate to it.
     */
    @Value
    @Builder
    public static class MethodCallInterceptor {

        @NonNull // Lombok creates runtime nullness check for this own annotation only
        Object proxyTarget;

        boolean proxyTargetIsController;

        @NonNull // Lombok creates runtime nullness check for this own annotation only
        Feature feature;

        @NonNull // Lombok creates runtime nullness check for this own annotation only
        Predicate<Feature> featureTester;

        @RuntimeType
        @Nullable
        @SuppressWarnings("java:S1160") // throw more than 1 checked exception
        public Object invoke(@Origin Method method, @AllArguments Object[] methodArguments)
                throws InvocationTargetException, IllegalAccessException {
            // don't cache feature state: it can be modified at any time manually, depend on IP address, date, etc.
            if (featureTester.test(feature)) {
                return method.invoke(proxyTarget, methodArguments);
            }

            return processDisabledFeature(feature, method);
        }

        /**
         * Behavior is described in outer class comment.
         */
        @Nullable
        private Object processDisabledFeature(Feature feature, Method method) {
            if (proxyTargetIsController) {
                var e = new FeatureDisabledException(feature);
                log.error("Feature '{}' disabled - throwing [{}] instead of calling endpoint [{}]",
                        feature, e.getClass().getSimpleName(), method);
                throw e;
            }

            // skip void method (no need for isAssignableFrom() for void class)
            if (method.getReturnType() == void.class) {
                log.warn("Feature '{}' disabled - skipping void method call [{}]", feature, method);
                return null;
            }

            var e = new FeatureDisabledException(feature);
            log.error("Feature '{}' disabled - throwing [{}] instead of calling [{}]", feature,
                    e.getClass().getSimpleName(), method);
            throw e;
        }

    }

}
