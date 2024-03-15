package io.nuvalence.workmanager.service.domain.workflow;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Represents a workflow task.
 */
@Value
@Builder
public class WorkflowTask {
    String key;
    String name;

    @Singular List<WorkflowAction> actions;
}
