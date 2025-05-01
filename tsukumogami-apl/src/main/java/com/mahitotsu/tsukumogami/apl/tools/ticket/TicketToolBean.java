package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class TicketToolBean extends ActionGroupProperties implements TicketTool {

    public TicketToolBean() {
        super("TicketTool", """
                チケットの起票、検索、照会する機能を提供するツールです。
                """, Arrays.asList(
                new FunctionProperties("createTicket", """
                        指定された内容でチケットを起票し、作成したチケットの識別子を返します。識別子はUUIDです。
                        """,
                        Arrays.asList(
                                new ParameterProperties("1_title", "string", true, """
                                        作成するチケットのタイトル。チケットの内容を端的に表す簡潔な文章を指定します。
                                        """),
                                new ParameterProperties("2_dueDate", "string", true, """
                                        作成するチケットの期日。ISO形式で年月日まで指定します。
                                        """),
                                new ParameterProperties("3_description", "string", true, """
                                        作成するチケットの内容。具体的に曖昧さがない文章を指定します。
                                                """)))),
                TicketTool.class);
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
