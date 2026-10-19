CREATE TABLE categories(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE tickets(
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    due_at TIMESTAMP,
    closed_at TIMESTAMP
);
CREATE TABLE ticket_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO categories (name) VALUES ('Hardware'), ('Software'), ('Network');