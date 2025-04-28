package com.mahitotsu.tsukumogami.api.service.tickets;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;

import com.mahitotsu.tsukumogami.api.service.InlineAgentServiceBase;

import jakarta.annotation.PostConstruct;

@Service
@ConfigurationProperties(prefix = "tsukumogami.ticket-agent")
public class TicketAgent extends InlineAgentServiceBase {

    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    public void overwriteProperties() throws IOException {

        final String originalInstruction = this.getInstruction();
        final String ddl = this.resourceLoader.getResource("classpath:schema.sql")
                .getContentAsString(Charset.forName("utf-8"));

        final Properties props = new Properties();
        props.setProperty("ddl", ddl);

        final String newInstcution = new PropertyPlaceholderHelper("{", "}").replacePlaceholders(originalInstruction,
                props);
        this.setInstruction(newInstcution);
    }
}
