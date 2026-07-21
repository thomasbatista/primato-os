CREATE TABLE workers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    function VARCHAR(255),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    user_id BIGINT UNIQUE REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
