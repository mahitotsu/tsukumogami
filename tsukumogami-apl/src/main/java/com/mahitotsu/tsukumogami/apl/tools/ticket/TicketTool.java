package com.mahitotsu.tsukumogami.apl.tools.ticket;

import java.time.LocalDate;
import java.util.UUID;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupAttributes;
import com.mahitotsu.tsukumogami.apl.tools.FunctionAttributes;
import com.mahitotsu.tsukumogami.apl.tools.ParameterAttributes;

@ActionGroupAttributes(name = "TicketTool", description = """
        チケットの起票、検索、照会する機能を提供するツールです。
        """)
public interface TicketTool {

    @FunctionAttributes(name = "createTicket", description = """
            指定された内容でチケットを起票し、作成したチケットの識別子を返します。識別子はUUIDです。
            """)
    UUID createTicket(
            @ParameterAttributes(name = "title", description = """
                    作成するチケットのタイトル。チケットの内容を端的に表す簡潔な文章を指定します。
                    """, required = true, type = "string") String title,
            @ParameterAttributes(name = "duDate", description = """
                    作成するチケットの期日。ISO形式で年月日まで指定します。
                    """, required = true, type = "string") LocalDate dueDate,
            @ParameterAttributes(name = "description", description = """
                    作成するチケットの内容。具体的に曖昧さがない文章を指定します。
                    """, required = true, type = "string") String description);
}