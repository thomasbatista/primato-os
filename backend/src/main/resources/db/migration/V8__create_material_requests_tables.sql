CREATE SEQUENCE material_request_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE material_requests (
    id BIGSERIAL PRIMARY KEY,
    request_number BIGINT NOT NULL UNIQUE,
    work_order_id BIGINT REFERENCES work_orders(id),
    project_id BIGINT NOT NULL REFERENCES projects(id),
    request_date DATE NOT NULL,
    needed_by_date DATE,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    justification TEXT,
    notes TEXT,
    delivery_location VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN
        ('DRAFT', 'REQUESTED', 'APPROVED', 'PURCHASED', 'PARTIALLY_DELIVERED', 'DELIVERED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

ALTER SEQUENCE material_request_number_seq OWNED BY material_requests.request_number;

CREATE TABLE material_request_items (
    id BIGSERIAL PRIMARY KEY,
    material_request_id BIGINT NOT NULL REFERENCES material_requests(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity NUMERIC(14, 3) NOT NULL,
    unit VARCHAR(20) NOT NULL CHECK (unit IN
        ('UNIT', 'METER', 'SQUARE_METER', 'CUBIC_METER', 'KILOGRAM', 'BAG', 'BOX', 'SHEET', 'BAR', 'ROLL', 'LITER')),
    brand VARCHAR(255),
    photo_reference VARCHAR(1000),
    notes TEXT,
    quantity_delivered NUMERIC(14, 3) NOT NULL DEFAULT 0
);
