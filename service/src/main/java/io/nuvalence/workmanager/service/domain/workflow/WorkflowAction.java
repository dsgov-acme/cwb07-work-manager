package io.nuvalence.workmanager.service.domain.workflow;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a workflow action.
 */
@Data
@Builder
public class WorkflowAction {
    public static final String UI_CLASS_PRIMARY = "Primary";
    public static final String UI_CLASS_SECONDARY = "Secondary";
    public static final String UI_CLASS_ADVERSE = "Adverse";

    String key;
    String uiLabel;
    String uiClass;
    String modalContext;
    String modalButtonLabel;
}
