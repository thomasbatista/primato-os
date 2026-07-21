CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    client VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    responsible VARCHAR(255),
    start_date DATE,
    expected_deadline DATE,
    current_stage VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'PAUSED', 'FINISHED')),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
