package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.UUID;

public interface TicketTool {
    
    UUID createTicket(String title, LocalDate dueDate, String description);
}
