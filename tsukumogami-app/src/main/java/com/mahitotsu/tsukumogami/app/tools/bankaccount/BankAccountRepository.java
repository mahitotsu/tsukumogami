package com.mahitotsu.tsukumogami.app.tools.bankaccount;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccountEntity, String> {
    
}
