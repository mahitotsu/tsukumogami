CREATE TABLE ticket ( -- ticket table
    id CHAR(7) NOT NULL, -- id for the ticket
    title VARCHAR(32) NOT NULL, -- title of the ticket
    due_date DATE NOT NULL, -- due date of the ticket
    assigned_to VARCHAR(32), -- the person or organization that is assigned to the ticket
    PRIMARY KEY (id)
);