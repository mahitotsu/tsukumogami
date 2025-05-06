package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class TicketToolBean extends ActionGroupProperties implements TicketTool {

    public TicketToolBean() {
        super(TicketTool.class);
    }

    @Autowired
    private TicketRepository ticketRepository;

    @Transactional
    public UUID createTicket(final String title, final LocalDate dueDate, final String description) {

        final TicketEntity ticket = new TicketEntity();
        ticket.setId(UUID.randomUUID());
        ticket.setTitle(title);
        ticket.setDueDate(dueDate);
        ticket.setDescription(description);

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            ticket.setAssignee(auth.getName());
        }

        return this.ticketRepository.save(ticket).getId();
    }

    @Transactional(readOnly = true)
    public TicketEntity getTicket(final UUID ticketId) {
        return this.ticketRepository.findById(ticketId).orElse(null);
    }
}
