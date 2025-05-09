package com.mahitotsu.tsukumogami.apl.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import com.mahitotsu.tsukumogami.apl.TestBase;
import com.mahitotsu.tsukumogami.apl.tools.ticket.TicketToolBean;

public class AgentHostServiceTest extends TestBase {

    @Autowired
    private AgentHostService proteus;

    @Autowired
    private TicketToolBean ticketTool;

    // @Test
    public void test_WhatDayToday() {

        final String workOrder = """
                今日は何月何日の何曜日ですか。
                 """;
        this.proteus.execute(workOrder);
    }

    // @Test
    @WithMockUser(username = "Usr001@test.com")
    public void test_CreateTicket() {

        final String workOrder = """
                来週の金曜日に、本格的なインドカレーを作成したいと考えています。
                それまでに必要な食材を買いそろえる必要があります。
                このことを忘れずに実行するために今日期限のチケットを起票しておいてください。
                チケットには必要な食材のリストを調べた結果も記載しておいてください。
                最終行には起票したチケットのIDのみを記載してください。
                 """;
        final String resultReport = this.proteus.execute(workOrder);
        final String[] lines = resultReport.split("\\n");

        final String ticketId = lines[lines.length - 1];
        System.out.println(this.ticketTool.getTicket(UUID.fromString(ticketId)));
    }
}
