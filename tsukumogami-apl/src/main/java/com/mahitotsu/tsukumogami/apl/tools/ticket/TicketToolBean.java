package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class TicketToolBean extends ActionGroupProperties implements TicketTool {

    public TicketToolBean() {
        super(TicketTool.class);
    }

    public UUID createTicket(final String title, final LocalDate dueDate, final String description) {

        final Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTitle(title);
        ticket.setDueDate(dueDate);
        ticket.setDescription(description);

        return ticket.getId();
    }
}
