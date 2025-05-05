package com.mahitotsu.tsukumogami.apl.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.tsukumogami.apl.TestBase;

public class AgentHostServiceTest extends TestBase {

    @Autowired
    private AgentHostService proteus;

    @Test
    public void test_WhatDayToday() {

        final String workOrder = """
                今日は何月何日の何曜日ですか。
                 """;
        final String resultReport = this.proteus.execute(workOrder);
        System.out.println(resultReport);
    }

    @Test
    public void test_CreateTicket() {

        final String workOrder = """
                明日、本格的なインドカレーを作成したいと考えています。
                それまでに必要な食材を買いそろえる必要があります。
                このことを忘れずに実行するために今日期限のチケットを起票しておいてください。
                 """;
        final String resultReport = this.proteus.execute(workOrder);
        System.out.println(resultReport);
    }
}
