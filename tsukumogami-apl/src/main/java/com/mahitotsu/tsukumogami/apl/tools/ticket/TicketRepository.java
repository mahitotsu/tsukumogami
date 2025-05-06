package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

}
