package com.mahitotsu.tsukumogami.apl.tools.websearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class WebSearchToolBean extends ActionGroupProperties implements WebSearchTool {

    public WebSearchToolBean() {
        super(WebSearchTool.class);
    }

    @Value("${websearch.apikey}")
    private String apiKey;

    @Value("https://api.perplexity.ai/chat/completions")
    private String apiEndpoint;

    private RestOperations restClient = new RestTemplate();

    public String search(final String request, final String... domains) {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(this.apiKey);

        final Collection<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", """
                You are a helpful assistant.
                """));
        messages.add(Map.of("role", "user", "content", request));

        final Map<String, Object> body = new HashMap<>();
        body.put("model", "sonar");
        if (domains.length > 0) {
            body.put("requestsearch_domain_filter", String.join(",", domains));
        }
        body.put("messages", messages);

        final HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        final ResponseEntity<String> response = this.restClient.exchange(this.apiEndpoint, HttpMethod.POST,
                requestEntity, String.class);

        return response.getBody();
    }
}
