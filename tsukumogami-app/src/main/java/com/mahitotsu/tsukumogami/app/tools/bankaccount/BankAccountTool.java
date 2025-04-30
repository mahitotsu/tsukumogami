package com.mahitotsu.tsukumogami.app.tools.bankaccount;

import java.util.UUID;

public interface BankAccountTool {
    
    UUID deposit(String accountNumber, long amount);

    UUID withdraw(String accountNumber, long amount);

    long getBalance(String accountNumber);
}
