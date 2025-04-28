package com.mahitotsu.tsukumogami.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

@Component
@Qualifier("RdbOperations")
public class RdbOperations {

    @Autowired
    private JdbcOperations JdbcOperations;
    
    public List<Map<String,Object>> executeQuery(final String statement) {

        return this.JdbcOperations.queryForList("SELECT * FROM ticket");
    }
}
