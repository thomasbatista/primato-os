CREATE TABLE daily_reports (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    date DATE NOT NULL,
    filled_by_worker_id BIGINT NOT NULL REFERENCES workers(id),
    start_time TIME,
    end_time TIME,
    weather_condition VARCHAR(255),
    extra_services TEXT,
    problems_found TEXT,
    pending_issues TEXT,
    materials_used TEXT,
    materials_missing TEXT,
    next_day_forecast TEXT,
    notes TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'FINALIZED')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE daily_report_team_present (
    daily_report_id BIGINT NOT NULL REFERENCES daily_reports(id),
    worker_id BIGINT NOT NULL REFERENCES workers(id),
    PRIMARY KEY (daily_report_id, worker_id)
);

CREATE TABLE daily_report_items (
    id BIGSERIAL PRIMARY KEY,
    daily_report_id BIGINT NOT NULL REFERENCES daily_reports(id),
    activity_description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('EXECUTED', 'PARTIALLY_EXECUTED', 'NOT_EXECUTED', 'NOT_APPLICABLE')),
    reason VARCHAR(500),
    observation TEXT,
    new_expected_date DATE
);

CREATE TABLE daily_report_photos (
    id BIGSERIAL PRIMARY KEY,
    daily_report_id BIGINT NOT NULL REFERENCES daily_reports(id),
    url VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
