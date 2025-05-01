package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class Ticket {
    private UUID id;
    private String title;
    private String assignee;
    private LocalDate dueDate;
    private String description;
}
