package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.util.List;
import java.util.UUID;

public interface WorkOrderTool {
    
    List<String> listWorkorderIds(String condition);

    WorkOrderEntity describeWorkOrder (UUID workOrderId);

    UUID createWorkOrder(String title, String assignee, String instruction);

    UUID registerWorkResult(UUID workOrderId, String result);
}
