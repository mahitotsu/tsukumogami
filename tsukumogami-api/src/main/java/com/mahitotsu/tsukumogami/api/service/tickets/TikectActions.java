package com.mahitotsu.tsukumogami.api.service.tickets;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

@Component
@Qualifier("TicketActions")
public class TikectActions {

    @Autowired
    private JdbcOperations jdbcOperations;

    public List<Map<String, Object>> queryTickets(final String conditions) {
        return this.jdbcOperations.queryForList("SELECT * FROM ticket " + conditions);
    }
}
