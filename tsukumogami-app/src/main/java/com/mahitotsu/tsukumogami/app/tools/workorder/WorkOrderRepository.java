package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, UUID> {
    
}
