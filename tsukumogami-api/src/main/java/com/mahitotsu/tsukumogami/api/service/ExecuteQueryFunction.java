package com.mahitotsu.tsukumogami.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;

@Component
@Qualifier("RdbOperations.executeQuery")
public class ExecuteQueryFunction implements ActionGroupFunction {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String invoke(final List<FunctionParameter> parametersl) throws JsonProcessingException {

        final List<Map<String, Object>> resultSet = new ArrayList<>();
        resultSet.add(Map.of("id", UUID.randomUUID().toString(), "name", "user2"));
        resultSet.add(Map.of("id", UUID.randomUUID().toString(), "name", "user1"));

        return this.objectMapper.writeValueAsString(resultSet);
    }
}
