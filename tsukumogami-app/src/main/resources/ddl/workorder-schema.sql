CREATE TABLE work_order ( -- 作業指示を格納するテーブル。一レコードが一つの作業指示を表す。
    id CHAR(36) NOT NULL, -- 作業指示の一意な識別子。UUID形式の文字列。
    title VARCHAR(128) NOT NULL, -- 作業指示のタイトル。
    assignee VARCHAR(64), -- 作業指示の担当者。 64文字までのメールアドレス形式の文字列。
    instruction VARCHAR(1024), -- 作業指示の詳細な内容。
    closed CHAR(1) NOT NULL, -- 作業が完了しているかどうか。完了している場合は'Y'、それ以外の場合は'N'。
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作業指示が登録された日時。
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作業指示が更新された日時。
    PRIMARY KEY (id)
);

CREATE TABLE work_result ( -- 作業結果を格納するテーブル。一レコードが一つの作業結果を表す。単一の作業指示に対して複数の作業結果が登録可能。
    id CHAR(36) NOT NULL, -- 作業結果の一意な識別子。UUID形式の文字列。
    workorder_id CHAR(36) NOT NULL, -- 作業指示のID。workorderテーブルのidを参照。
    result VARCHAR(1024), -- 作業結果の詳細な内容。
    registed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 作業結果が登録された日時。
    PRIMARY KEY (id),
    FOREIGN KEY (workorder_id) REFERENCES work_order(id)
);