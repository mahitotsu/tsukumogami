package com.mahitotsu.tsukumogami.app.tools.bankaccount;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity(name="BankAccountTransactionEntity")
@Table(name="bank_account_transaction")
@Data
public class BankAccountTransactionEntity {

    @Id
    @Column(name="id", updatable = false)
    private UUID id;

    @Column(name="account_number")
    @Pattern(regexp = "[0-9]{10}")
    private String accountNumber;

    @Max(999999999999L)
    @Min(-999999999999L)
    private long amount;
}
