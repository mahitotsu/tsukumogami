package com.mahitotsu.tsukumogami.app.tools.bankaccount;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity(name = "BankAccount")
@Table(name = "bank_account")
@Data
public class BankAccountEntity {

    @Id
    @Column(name = "account_number")
    @Pattern(regexp = "[0-9]{10}")
    private String accountNumber;

    @Column(name = "balance")
    @Max(9999999999999999L)
    @Min(0)
    private long balance;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number", referencedColumnName = "account_number")
    @OrderBy("created_at")
    private List<BankAccountTransactionEntity> transactions;
}
