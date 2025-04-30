package com.mahitotsu.tsukumogami.app.tools.bankaccount;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mahitotsu.tsukumogami.app.tools.ToolBase;

@Component
public class BankAccountToolBean extends ToolBase implements BankAccountTool {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Transactional
    @Override
    public UUID deposit(final String accountNumber, final long amount) {

        if (amount < 0) {
            throw new IllegalArgumentException("The amount must not be negative value.");
        }

        final BankAccountEntity bankAccount = this.bankAccountRepository.findById(accountNumber).get();
        final long newBalance = bankAccount.getBalance() + amount;

        final BankAccountTransactionEntity transaction = new BankAccountTransactionEntity();
        transaction.setId(UUID.randomUUID());
        transaction.setAccountNumber(accountNumber);
        transaction.setAmount(amount);

        bankAccount.setBalance(newBalance);
        bankAccount.getTransactions().add(transaction);

        return transaction.getId();
    }

    @Transactional
    @Override
    public UUID withdraw(final String accountNumber, final long amount) {

        if (amount < 0) {
            throw new IllegalArgumentException("The amount must not be negative value.");
        }

        final BankAccountEntity bankAccount = this.bankAccountRepository.findById(accountNumber).get();
        final long newBalance = bankAccount.getBalance() - amount;

        final BankAccountTransactionEntity transaction = new BankAccountTransactionEntity();
        transaction.setId(UUID.randomUUID());
        transaction.setAccountNumber(accountNumber);
        transaction.setAmount(amount * -1);

        bankAccount.setBalance(newBalance);
        bankAccount.getTransactions().add(transaction);

        return transaction.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public long getBalance(final String accountNumber) {
        return this.bankAccountRepository.findById(accountNumber).get().getBalance();
    }

}
