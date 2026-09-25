package com.devpilot.backend.dto;

/**
 * Represents a single test suggestion.
 */
public class AiTestSuggestionDto {

    private TestType testType;
    private TestPriority priority;
    private String title;
    private String description;
    private String targetMethod;
    private String scenario;
    private String expectedBehavior;

    public AiTestSuggestionDto() {
    }

    public AiTestSuggestionDto(TestType testType, TestPriority priority, String title,
                               String description, String targetMethod, String scenario,
                               String expectedBehavior) {
        this.testType = testType;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.targetMethod = targetMethod;
        this.scenario = scenario;
        this.expectedBehavior = expectedBehavior;
    }

    public TestType getTestType() {
        return testType;
    }

    public void setTestType(TestType testType) {
        this.testType = testType;
    }

    public TestPriority getPriority() {
        return priority;
    }

    public void setPriority(TestPriority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    public void setExpectedBehavior(String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }
}
