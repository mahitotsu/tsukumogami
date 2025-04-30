CREATE TABLE bank_account ( -- 銀行口座の基本属性と残高を格納するテーブル。一レコードが一口座を表す。
    account_number CHAR(10) NOT NULL, -- 口座を一意に識別するための識別子。数字のみで構成された10桁の文字列。
    balance DECIMAL(16), -- 口座の残高。正の整数が格納される想定。負数は行エラーとして扱うべき項目。
    PRIMARY KEY (account_number)
);

CREATE TABLE bank_account_transaction ( -- 銀行口座で発生した取引履歴を格納するテーブル。一レコードが一取引を表す。
    id CHAR(36) NOT NULL, -- 取引を一意に識別するための識別子。UUID形式の文字列。
    account_number CHAR(10) NOT NULL, -- 取引が発生した口座の識別子。
    amount DECIMAL(12) NOT NULL, -- 取引金額。預け入れの場合は正数、払い出しの場合は負数。
    new_balance DECIMAL(16) NOT NULL, -- 取引後の新しい残高。
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 取引の発生日時
    FOREIGN KEY (account_number) REFERENCES bank_account(bank_account),
    PRIMARY KEY (id)
);