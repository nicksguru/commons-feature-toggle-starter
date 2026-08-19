package guru.nicks.commons.feature;

import guru.nicks.commons.feature.exception.FeatureDisabledException;
import guru.nicks.commons.utils.ExceptionUtils;
import guru.nicks.commons.utils.ReflectionUtils;

import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatcher;
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
 * <b>WARNING: As of Spring Boot 3.5.8, controllers cannot be wrapped:</b> endpoints returning void (e.g. those having
 * {@code DeleteMapping}) stop being called - no matter if the feature is enabled or not. The reason is unknown. For
 * this reason, this post processor throws an exception when applied to a controller.
 * <p>
 * {@code equals()}, {@code hashCode()} and {@code toString()} (canonical signatures) are intercepted and delegated to
 * the wrapped bean regardless of the feature state: {@code wrapper.equals(x)} is {@code true} if and only if {@code x}
 * is the wrapper itself or {@code target.equals(x)}; {@code wrapper.hashCode() == target.hashCode()}. The reverse
 * direction ({@code target.equals(wrapper)}) cannot be supported because the wrapper does not mirror the target's field
 * state - never mix raw and wrapped instances in the same collection.
 * <p>
 * {@code compareTo()}, {@code clone()}, and {@code finalize()} are not intercepted. Final overrides of
 * {@code equals()}, {@code hashCode()} or {@code toString()} in the target class cannot be delegated (ByteBuddy cannot
 * override final methods) and keep their inherited behavior.
 * <p>
 * Other public methods of annotated Spring beans, if the specific feature is disabled (as per {@link FeatureTester})
 * <b>at the moment of the call</b>:
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

    /**
     * Methods intercepted by the generated wrapper: all public methods not declared in {@link Object} class, plus
     * {@code equals()}, {@code hashCode()} and {@code toString()}. Note the matcher's left-associativity:
     * {@code (isPublic AND NOT isDeclaredByObject) OR isEquals OR isHashCode OR isToString}. The latter three match
     * canonical signatures only, so user overloads such as {@code equals(String)} are not matched.
     */
    private static final ElementMatcher<MethodDescription> INTERCEPTED_METHODS = ElementMatchers.isPublic()
            .and(not(ElementMatchers.isDeclaredBy(Object.class)))
            .or(ElementMatchers.isEquals())
            .or(ElementMatchers.isHashCode())
            .or(ElementMatchers.isToString());
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
                .method(INTERCEPTED_METHODS)
                // without the filter, ByteBuddy's MethodNameEqualityResolver would bind the intercepted
                // equals()/hashCode()/toString() to the interceptor's own Lombok-generated methods
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .filter(ElementMatchers.named("invoke"))
                        .to(interceptor))
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

        /**
         * Checks if the given method is one of {@link Object}'s {@code equals()}, {@code hashCode()} or
         * {@code toString()} having the canonical signature.
         *
         * @param method method to check
         * @return {@code true} method is a canonical {@link Object} method
         */
        private static boolean isObjectMethod(Method method) {
            return switch (method.getName()) {
                case "equals" -> (method.getParameterCount() == 1)
                        && (method.getParameterTypes()[0] == Object.class);
                case "hashCode", "toString" -> method.getParameterCount() == 0;
                default -> false;
            };
        }

        @RuntimeType
        @Nullable
        @SneakyThrows
        public Object invoke(@This Object wrapper, @Origin Method method, @AllArguments Object[] methodArguments) {
            // equality must not depend on a feature toggle: delegate Object methods before checking the feature state
            if (isObjectMethod(method)) {
                if ("equals".equals(method.getName())) {
                    return delegateEquals(wrapper, methodArguments[0]);
                }

                return invokeOnTarget(method, methodArguments);
            }

            // don't cache feature state: it can be modified at any time manually, depend on IP address, date, etc.
            if (featureTester.test(feature)) {
                return invokeOnTarget(method, methodArguments);
            }

            return processDisabledFeature(feature, method);
        }

        /**
         * Invokes the given method on the wrapped target bean.
         *
         * @param method          method to invoke
         * @param methodArguments method arguments
         * @return method invocation result
         */
        @Nullable
        @SneakyThrows
        private Object invokeOnTarget(Method method, Object[] methodArguments) {
            try {
                return method.invoke(proxyTarget, methodArguments);
            }
            // without this, callers that catch e.g. BusinessException will never match because it's wrapped
            catch (InvocationTargetException e) {
                throw ExceptionUtils.unwrapInvocationTargetException(e);
            }
        }

        /**
         * Delegates {@code equals()} to the wrapped target bean, adding reflexivity for the wrapper itself.
         * <p>
         * The resulting equality is asymmetric: {@code target.equals(wrapper)} returns {@code false} because the
         * wrapper does not mirror the target's field state - never mix raw and wrapped instances in the same
         * collection.
         *
         * @param wrapper generated wrapper the method was invoked on
         * @param other   object to compare the wrapper against
         * @return comparison result
         */
        private boolean delegateEquals(Object wrapper, @Nullable Object other) {
            // reflexivity: proxyTarget.equals(wrapper) would compare against the wrapper's null/default fields
            if (other == wrapper) {
                return true;
            }

            return proxyTarget.equals(other);
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
