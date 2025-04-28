CREATE TABLE ticket ( -- チケットが格納されているテーブル
    id CHAR(7) NOT NULL, -- チケットの識別子。
    title VARCHAR(32) NOT NULL, -- チケットのタイトル。チケットの内容を簡潔に表現する。
    due_date DATE NOT NULL, -- チケットの作業期限。
    assigned_to VARCHAR(32), -- チケットの担当者。ユーザーのユーザー名を設定する。未設定の場合はNULL。
    PRIMARY KEY (id)
);