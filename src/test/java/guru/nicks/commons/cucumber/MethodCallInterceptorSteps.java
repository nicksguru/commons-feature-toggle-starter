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
import lombok.RequiredArgsConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.togglz.core.Feature;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link FeatureBeanPostProcessor.MethodCallInterceptor}.
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

        var lastException = catchThrowable(() -> interceptor.invoke(methodToInvoke, new Object[]{}));
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

}
