package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Entity(name = "Ticket")
@Data
public class TicketEntity {

    @Id
    private UUID id;

    @Column(length = 64)
    private String title;

    @Column(length = 32)
    private String assignee;

    @Temporal(TemporalType.DATE)
    private LocalDate dueDate;

    @Column(length = 1024)
    private String description;
}
