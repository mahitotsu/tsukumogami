package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class TicketToolBean extends ActionGroupProperties implements TicketTool {

    public TicketToolBean() {
        super(TicketTool.class);
    }

    private final Map<UUID, Ticket> tickets = new HashMap<>();

    public UUID createTicket(final String title, final LocalDate dueDate, final String description) {

        final Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTitle(title);
        ticket.setDueDate(dueDate);
        ticket.setDescription(description);

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            ticket.setAssignee(auth.getName());
        }

        this.tickets.put(ticket.getId(), ticket);
        return ticket.getId();
    }

    public Ticket getTicket(final UUID ticketId) {
        return this.tickets.get(ticketId);
    }
}
