package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.feature.FeatureBeanPostProcessor;
import guru.nicks.commons.feature.FeatureTester;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.togglz.core.Feature;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link FeatureBeanPostProcessor.MethodCallInterceptor} and the wrapper class generated
 * by {@link FeatureBeanPostProcessor}.
 */
@RequiredArgsConstructor
public class MethodCallInterceptorSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private Feature feature;
    @Mock
    private FeatureTester featureTester;
    @Spy
    private ProxyTarget proxyTarget;
    private AutoCloseable closeableMocks;

    private FeatureBeanPostProcessor.MethodCallInterceptor interceptor;
    private boolean proxyTargetIsController;
    private boolean returnVoid;

    private TestBean rawBean;
    private Object wrappedBean;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a feature is enabled: {booleanValue}")
    public void aFeatureIs(boolean featureState) {
        when(featureTester.test(feature))
                .thenReturn(featureState);
    }

    @And("a proxy target is a {word}")
    public void aProxyTargetIsA(String targetType) {
        this.proxyTargetIsController = "controller".equals(targetType);
    }

    @And("a proxy target method returns {word}")
    public void aProxyTargetMethodReturns(String returnType) {
        returnVoid = "void".equals(returnType);
    }

    @When("the interceptor is invoked")
    public void theInterceptorIsInvoked() throws NoSuchMethodException {
        interceptor = FeatureBeanPostProcessor.MethodCallInterceptor.builder()
                .proxyTarget(proxyTarget)
                .proxyTargetIsController(proxyTargetIsController)
                .feature(feature)
                .featureTester(featureTester)
                .build();

        Method methodToInvoke = returnVoid
                ? proxyTarget.getClass().getMethod("voidMethod")
                : proxyTarget.getClass().getMethod("nonVoidMethod");

        // passing the target as the wrapper is harmless: the identity branch never triggers for these methods
        var lastException = catchThrowable(() -> interceptor.invoke(proxyTarget, methodToInvoke, new Object[]{}));
        textWorld.setLastException(lastException);
    }

    @Then("the proxy target method should be called")
    public void theProxyTargetMethodShouldBeCalled() {
        if (returnVoid) {
            verify(proxyTarget).voidMethod();
        } else {
            verify(proxyTarget).nonVoidMethod();
        }
    }

    @Then("the proxy target method should not be called")
    public void theProxyTargetMethodShouldNotBeCalled() {
        if (returnVoid) {
            verify(proxyTarget, never()).voidMethod();
        } else {
            verify(proxyTarget, never()).nonVoidMethod();
        }
    }

    @When("the bean is wrapped by the post processor")
    public void theBeanIsWrappedByThePostProcessor() {
        rawBean = new TestBean("some-value");
        var postProcessor = new TestFeaturePostProcessor(featureTester, feature);
        wrappedBean = postProcessor.postProcessAfterInitialization(rawBean, "rawBean");

        assertThat(wrappedBean)
                .as("wrapping must actually happen")
                .isNotSameAs(rawBean)
                .isInstanceOf(TestBean.class);
    }

    @Then("the wrapper should equal itself")
    public void theWrapperShouldEqualItself() {
        assertThat(wrappedBean.equals(wrappedBean))
                .as("wrapper.equals(wrapper)")
                .isTrue();
    }

    @Then("the wrapper should equal the raw target")
    public void theWrapperShouldEqualTheRawTarget() {
        assertThat(wrappedBean.equals(rawBean))
                .as("wrapper.equals(rawBean)")
                .isTrue();
    }

    @Then("the wrapper hashCode should equal the raw target hashCode")
    public void theWrapperHashCodeShouldEqualTheRawTargetHashCode() {
        assertThat(wrappedBean)
                .as("wrapper.hashCode()")
                .hasSameHashCodeAs(rawBean);
    }

    @Then("a HashSet containing the raw target should contain the wrapped bean")
    public void aHashSetContainingTheRawTargetShouldContainTheWrappedBean() {
        Set<Object> set = new HashSet<>();
        set.add(rawBean);

        // HashSet.contains() applies the QUERY's equals() (HashMap.getNode checks key.equals(storedKey)), i.e.
        // wrapper.equals(rawBean) which is delegated to the target. AssertJ's contains() iterates and applies the
        // STORED element's equals() (rawBean.equals(wrapper)) instead, flipping the documented asymmetry
        assertThat(set.contains(wrappedBean))
                .as("HashSet(rawBean).contains(wrapper)")
                .isTrue();
    }

    @Then("a HashSet containing the wrapped bean should not contain the raw target")
    public void aHashSetContainingTheWrappedBeanShouldNotContainTheRawTarget() {
        Set<Object> set = new HashSet<>();
        set.add(wrappedBean);

        // documented asymmetry: contains() invokes the query's equals(), i.e. rawBean.equals(wrapper), which compares
        // against the wrapper's null/default fields; AssertJ's doesNotContain() can't be used for the same reason as
        // above - it would apply the stored wrapper's delegated equals()
        assertThat(set.contains(rawBean))
                .as("HashSet(wrapper).contains(rawBean)")
                .isFalse();
    }

    @Then("the wrapper toString should equal the raw target toString")
    public void theWrapperToStringShouldEqualTheRawTargetToString() {
        assertThat(wrappedBean)
                .as("wrapper.toString()")
                .hasToString(rawBean.toString());
    }

    /**
     * A dummy class to be used as a proxy target for testing.
     */
    public static class ProxyTarget {

        public void voidMethod() {
            // A method that returns void.
        }

        public String nonVoidMethod() {
            return "some-value";
        }

    }

    /**
     * Value-style test bean. Unlike a record or a {@code @Value} class, it's not final and can therefore be wrapped by
     * {@link FeatureBeanPostProcessor}. Must stay public: ByteBuddy defines the generated subclass in its own class
     * loader, so package-private visibility would not be accessible to it.
     * <p>
     * {@code doNotUseGetters = true} makes equals() read the field directly (like records and handwritten equals do).
     * With the default getter-based access, the wrapper's intercepted getValue() would delegate to the target and hide
     * the documented asymmetry: target.equals(wrapper) must compare against the wrapper's null/default fields.
     */
    @Getter
    @EqualsAndHashCode(doNotUseGetters = true)
    @ToString
    @RequiredArgsConstructor
    public static class TestBean {

        private final String value;

    }

    /**
     * Minimal {@link FeatureBeanPostProcessor} implementation recognizing {@link TestBean} only.
     */
    private static class TestFeaturePostProcessor extends FeatureBeanPostProcessor {

        private final Feature requiredFeature;

        private TestFeaturePostProcessor(Predicate<Feature> featureTester, Feature requiredFeature) {
            super(featureTester);
            this.requiredFeature = requiredFeature;
        }

        @Override
        public Optional<Feature> findRequiredFeature(Class<?> clazz) {
            return TestBean.class.equals(clazz)
                    ? Optional.of(requiredFeature)
                    : Optional.empty();
        }

    }

}
