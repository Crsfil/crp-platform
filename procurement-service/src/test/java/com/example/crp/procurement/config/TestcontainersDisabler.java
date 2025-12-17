package com.example.crp.procurement.config;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 condition that disables Testcontainers-based tests
 * when the system property {@code testcontainers.disabled} is set to {@code true}.
 */
public class TestcontainersDisabler implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Testcontainers enabled");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (Boolean.getBoolean("testcontainers.disabled")) {
            return ConditionEvaluationResult.disabled("Disabled via system property testcontainers.disabled=true");
        }
        return ENABLED;
    }
}
