package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity(name="WorkResult")
@Table(name="work_result")
@Data
public class WorkResultEntity {
    
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "work_order_id")
    private UUID workOrderId;

    @Column(name = "result")
    private String result;

    @Column(name = "registered_at", insertable = false, updatable = false)
    private LocalDateTime registeredAt;
}
