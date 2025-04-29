package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mahitotsu.tsukumogami.app.tools.ToolBase;

@Component("WorkOrderTool")
@ConfigurationProperties(prefix = "tsukumogami.tools.workorder")
public class WorkOrderToolBean extends ToolBase implements WorkOrderTool {

    @Autowired
    private JdbcOperations jdbcClient;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkResultRepository workResultRepository;

    @Transactional(readOnly = true)
    @Override
    public List<String> listWorkorderIds(final String condition) {
        return this.jdbcClient.queryForList("SELECT id FROM work_order " + condition, String.class);
    }

    @Transactional(readOnly = true)
    @Override
    public WorkOrderEntity describeWorkOrder(final UUID workOrderId) {
        return this.workOrderRepository.findById(workOrderId).get();
    }

    @Transactional
    @Override
    public UUID createWorkOrder(final String title, final String assignee, final String instruction) {

        final WorkOrderEntity workOrder = new WorkOrderEntity();
        workOrder.setId(UUID.randomUUID());
        workOrder.setTitle(title);
        workOrder.setAssignee(assignee);
        workOrder.setInstruction(instruction);

        return this.workOrderRepository.save(workOrder).getId();
    }

    @Transactional
    @Override
    public UUID registerWorkResult(final UUID workOrderId, final String result) {

        final WorkResultEntity workResult = new WorkResultEntity();
        workResult.setId(UUID.randomUUID());
        workResult.setWorkOrderId(workOrderId);
        workResult.setResult(result);

        return this.workResultRepository.save(workResult).getId();
    }
}