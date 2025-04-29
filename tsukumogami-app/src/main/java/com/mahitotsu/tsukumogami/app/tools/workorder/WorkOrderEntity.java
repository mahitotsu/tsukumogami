package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity(name="WorkOrder")
@Table(name="work_order")
@Data
public class WorkOrderEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "instruction")
    private String instruction;

    @Column(name = "result")
    private boolean closed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
