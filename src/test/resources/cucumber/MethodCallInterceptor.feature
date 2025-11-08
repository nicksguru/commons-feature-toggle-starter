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
