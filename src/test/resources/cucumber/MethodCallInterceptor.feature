#@disabled
Feature: Method Call Interceptor

  Scenario Outline: Intercepting method calls based on feature state
    Given a feature is enabled: <featureEnabled>
    And a proxy target is a <targetType>
    And a proxy target method returns <returnType>
    When the interceptor is invoked
    Then the proxy target method should <invocationExpectation>
    And the exception should be of type "<exceptionType>"
    Examples:
      | featureEnabled | targetType | returnType | invocationExpectation | exceptionType            |
      | true           | controller | void       | be called             |                          |
      | true           | controller | non-void   | be called             |                          |
      | true           | bean       | void       | be called             |                          |
      | true           | bean       | non-void   | be called             |                          |
      | false          | controller | void       | not be called         | FeatureDisabledException |
      | false          | controller | non-void   | not be called         | FeatureDisabledException |
      | false          | bean       | void       | not be called         |                          |
      | false          | bean       | non-void   | not be called         | FeatureDisabledException |

  Scenario: Wrapper equals itself and the raw target
    Given a feature is enabled: true
    When the bean is wrapped by the post processor
    Then the wrapper should equal itself
    And the wrapper should equal the raw target

  Scenario: Wrapper hashCode equals the raw target hashCode
    Given a feature is enabled: true
    When the bean is wrapped by the post processor
    Then the wrapper hashCode should equal the raw target hashCode

  Scenario: HashSet containing the raw target contains the wrapped bean
    Given a feature is enabled: true
    When the bean is wrapped by the post processor
    Then a HashSet containing the raw target should contain the wrapped bean

  Scenario: Documented asymmetry - HashSet containing the wrapped bean does not contain the raw target
    Given a feature is enabled: true
    When the bean is wrapped by the post processor
    Then a HashSet containing the wrapped bean should not contain the raw target

  Scenario: Wrapper toString delegates to the raw target
    Given a feature is enabled: true
    When the bean is wrapped by the post processor
    Then the wrapper toString should equal the raw target toString

  Scenario: Object methods are delegated even while the feature is disabled
    Given a feature is enabled: false
    When the bean is wrapped by the post processor
    Then the wrapper should equal itself
    And the wrapper should equal the raw target
    And the wrapper hashCode should equal the raw target hashCode
    And the wrapper toString should equal the raw target toString
