package com.flowlab.engine;

public interface Step {

    /**
     * Unique identifier matching the step name in workflow definitions.
     */
    String getName();

    /**
     * Executes the business logic for this step.
     * Can read and mutate the provided context.
     *
     * @param context the step context containing executionId, input payload, etc.
     * @return StepResult (SUCCESS, RETRY, or FAIL)
     * @throws Exception on unexpected errors
     */
    StepResult execute(StepContext context) throws Exception;

    /**
     * SAGA Compensation hook.
     * Reverses the side effects of execute() if a downstream step fails permanently.
     *
     * @param context the step context
     * @throws Exception on compensation error
     */
    default void compensate(StepContext context) throws Exception {
        // Default: no-op compensation
    }
}
