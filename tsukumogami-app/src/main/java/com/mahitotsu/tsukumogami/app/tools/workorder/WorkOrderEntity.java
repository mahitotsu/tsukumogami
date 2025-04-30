package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.time.LocalDateTime;
import java.util.UUID;

import com.mahitotsu.tsukumogami.app.jpa.OnUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity(name = "WorkOrder")
@Table(name = "work_order")
@Data
public class WorkOrderEntity {

    public static enum Status {
        TODO, IN_PROGRESS, COMPLETED
    }

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "title")
    @NotBlank
    private String title;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "instruction")
    private String instruction;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    @NotNull(groups = {OnUpdate.class})
    private LocalDateTime updatedAt;

    @Column(name = "wo_status")
    @NotNull
    private Status status;
}
