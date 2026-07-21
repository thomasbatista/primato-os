CREATE SEQUENCE work_order_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number BIGINT NOT NULL UNIQUE,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    date DATE NOT NULL,
    responsible_user_id BIGINT NOT NULL REFERENCES users(id),
    stage VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    description TEXT NOT NULL,
    daily_goal TEXT,
    planned_start_time TIME,
    planned_end_time TIME,
    materials_needed TEXT,
    tools TEXT,
    safety_guidelines TEXT,
    quality_criteria TEXT,
    notes TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'RELEASED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER SEQUENCE work_order_number_seq OWNED BY work_orders.order_number;

CREATE TABLE work_order_workers (
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    worker_id BIGINT NOT NULL REFERENCES workers(id),
    PRIMARY KEY (work_order_id, worker_id)
);
