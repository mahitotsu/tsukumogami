package com.mahitotsu.tsukumogami.app.agents.bankaccount;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mahitotsu.tsukumogami.app.agents.AgentBase;
import com.mahitotsu.tsukumogami.app.tools.bankaccount.BankAccountToolBean;
import com.mahitotsu.tsukumogami.app.tools.workorder.WorkOrderToolBean;

import jakarta.annotation.PostConstruct;

@Service
public class BankAccountAgent extends AgentBase {
    
    @Autowired
    private BankAccountToolBean bankAccountTool;

    @Autowired
    private WorkOrderToolBean workOrderTool;

    @PostConstruct
    public void setup() {
        this.registerToolAsActionGroup("WorkOrderTool", this.workOrderTool);
        this.registerToolAsActionGroup("BankAccountTool", this.bankAccountTool);
    }
}
