package com.mahitotsu.tsukumogami.apl.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.tsukumogami.apl.TestBase;

public class ProteusTest extends TestBase {

    @Autowired
    private Proteus proteus;

    @Test
    public void testExecute() {

        final String workOrder = """
                現時点で期限が今週末で自分自身に割り当てられているチケットが何件あるか調べてください。
                 """;
        final String resultReport = this.proteus.execute(workOrder);
        System.out.println(resultReport);
    }
}
