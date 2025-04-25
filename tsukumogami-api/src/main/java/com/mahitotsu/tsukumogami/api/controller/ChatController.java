package com.mahitotsu.tsukumogami.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mahitotsu.tsukumogami.api.service.AgentService;

import lombok.Data;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Data
    public static class ChatRequest {
        public String userInput;
    }

    @Autowired
    private AgentService agentService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public Map<String, String> getChatResponse(@RequestBody final ChatRequest request) {

        final String response = this.agentService.getChatResponse(request.userInput);

        final Map<String, String> responseMap = new HashMap<>();
        responseMap.put("response", response);

        return responseMap;
    }
}
