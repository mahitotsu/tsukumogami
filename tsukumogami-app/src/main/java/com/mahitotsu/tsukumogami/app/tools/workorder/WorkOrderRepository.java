package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, UUID> {

    public static final String FIND_NEXT_WORKORDER_QUERY = """
                SELECT wo FROM WorkOrder wo WHERE
                    wo.status = com.mahitotsu.tsukumogami.app.tools.workorder.WorkOrderEntity.Status.TODO
                    AND wo.assignee = :asignee
            """;

    @Query(FIND_NEXT_WORKORDER_QUERY)
    Optional<WorkOrderEntity> findNextWorkOrder(@Param("assignee") String assignee);
}
