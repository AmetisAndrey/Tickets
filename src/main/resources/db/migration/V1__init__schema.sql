
CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         category_id BIGINT NOT NULL REFERENCES categories(id),
                         priority VARCHAR(16) NOT NULL,
                         status VARCHAR(16) NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL,
                         due_at TIMESTAMPTZ NOT NULL,
                         closed_at TIMESTAMPTZ
);

CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_category ON tickets(category_id);

CREATE TABLE ticket_status_history (
                                       id BIGSERIAL PRIMARY KEY,
                                       ticket_id BIGINT NOT NULL REFERENCES tickets(id),
                                       from_status VARCHAR(16),
                                       to_status VARCHAR(16) NOT NULL,
                                       changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_history_ticket ON ticket_status_history(ticket_id);

INSERT INTO categories (name)
VALUES ('Hardware'), ('Software'), ('Network'), ('Access');
